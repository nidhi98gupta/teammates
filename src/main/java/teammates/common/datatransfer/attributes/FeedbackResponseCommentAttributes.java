package teammates.common.datatransfer.attributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.appengine.api.datastore.Text;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.util.Const;
import teammates.common.util.FieldValidator;
import teammates.common.util.JsonUtils;
import teammates.common.util.Logger;
import teammates.common.util.SanitizationHelper;
import teammates.storage.entity.FeedbackResponseComment;

/**
 * Represents a data transfer object for {@link FeedbackResponseComment} entities.
 */
public class FeedbackResponseCommentAttributes extends EntityAttributes<FeedbackResponseComment> {
    private static final Logger log = Logger.getLogger();

    // Required fields
    public String courseId;
    public String feedbackSessionName;
    // commentGiver holds email of student/instructor if comment giver is student/instructor
    // and name of team if comment giver is a team
    public String commentGiver;
    public Text commentText;

    // Optional fields
    public String feedbackResponseId;
    public String feedbackQuestionId;
    public List<FeedbackParticipantType> showCommentTo;
    public List<FeedbackParticipantType> showGiverNameTo;
    public boolean isVisibilityFollowingFeedbackQuestion;
    public Instant createdAt;
    public String lastEditorEmail;
    public Instant lastEditedAt;
    public Long feedbackResponseCommentId;
    public String giverSection;
    public String receiverSection;
    public FeedbackParticipantType commentGiverType;

    FeedbackResponseCommentAttributes() {
        giverSection = Const.DEFAULT_SECTION;
        receiverSection = Const.DEFAULT_SECTION;
        showCommentTo = new ArrayList<>();
        showGiverNameTo = new ArrayList<>();
        isVisibilityFollowingFeedbackQuestion = true;
        createdAt = Instant.now();
    }

    public static FeedbackResponseCommentAttributes valueOf(FeedbackResponseComment comment) {
        return builder(comment.getCourseId(), comment.getFeedbackSessionName(),
                    comment.getGiverEmail(), comment.getCommentText())
                .withFeedbackResponseId(comment.getFeedbackResponseId())
                .withFeedbackQuestionId(comment.getFeedbackQuestionId())
                .withFeedbackResponseCommentId(comment.getFeedbackResponseCommentId())
                .withCreatedAt(comment.getCreatedAt())
                .withGiverSection(comment.getGiverSection())
                .withReceiverSection(comment.getReceiverSection())
                .withCommentGiverType(comment.getCommentGiverType())
                .withLastEditorEmail(comment.getLastEditorEmail())
                .withLastEditedAt(comment.getLastEditedAt())
                .withVisibilityFollowingFeedbackQuestion(comment.getIsVisibilityFollowingFeedbackQuestion())
                .withShowCommentTo(comment.getShowCommentTo())
                .withShowGiverNameTo(comment.getShowGiverNameTo())
                .build();
    }

    /**
     * Returns new builder instance with default values for optional fields.
     *
     * <p>Following default values are set to corresponding attributes:
     * <ul>
     * <li>{@code giverSection = "None"}</li>
     * <li>{@code receiverSection = "None"}</li>
     * <li>{@code showCommentTo = new ArrayList<>()}</li>
     * <li>{@code showGiverNameTo = new ArrayList<>()}</li>
     * <li>{@code isVisibilityFollowingFeedbackQuestion = true}</li>
     * </ul>
     */
    public static Builder builder(String courseId, String feedbackSessionName, String commentGiver, Text commentText) {
        return new Builder(courseId, feedbackSessionName, commentGiver, commentText);
    }

    public boolean isVisibleTo(FeedbackParticipantType viewerType) {
        return showCommentTo.contains(viewerType);
    }

    public Long getId() {
        return feedbackResponseCommentId;
    }

    /**
     * Converts comment text in form of string i.e if it contains image, changes it into link.
     * This function is used for showing comment in csv and in feedback results table. Thus it is sanitized accordingly.
     * @param isCommentForCsv is comment meant for Csv
     * @return Comment in form of string
     */
    public String convertCommentTextToString(boolean isCommentForCsv) {
        String htmlText = commentText.getValue();
        StringBuilder comment = new StringBuilder(200);
        comment.append(Jsoup.parse(htmlText).text());
        if (!(Jsoup.parse(htmlText).getElementsByTag("img").isEmpty())) {
            comment.append("Images Link: ");
            Elements ele = Jsoup.parse(htmlText).getElementsByTag("img");
            for (Element element : ele) {
                comment.append(element.absUrl("src") + ' ');
            }
        }
        if (isCommentForCsv) {
            return SanitizationHelper.sanitizeForCsv(comment.toString());
        }
        return SanitizationHelper.sanitizeForHtml(comment.toString());
    }

    /**
     * Use only to match existing and known Comment.
     */
    public void setId(Long id) {
        this.feedbackResponseCommentId = id;
    }

    /**
     * Used to convert comment giver obtained from request parameter to appropriate FeedbackParticipantType.
     * @param commentGiverTypeString comment giver type
     * @return comment giver type as FeedbackParticipantType
     */
    public FeedbackParticipantType getCommentGiverTypeFromString(String commentGiverTypeString) {
        if (commentGiverTypeString.equals(Const.STUDENT)) {
            return FeedbackParticipantType.STUDENTS;
        }
        if (commentGiverTypeString.equals(Const.INSTRUCTOR)) {
            return FeedbackParticipantType.INSTRUCTORS;
        }
        if (commentGiverTypeString.equals(Const.TEAM)) {
            return FeedbackParticipantType.TEAMS;
        }
        log.severe("Unknown comment giver type");
        return null;
    }

    @Override
    public List<String> getInvalidityInfo() {
        FieldValidator validator = new FieldValidator();
        List<String> errors = new ArrayList<>();

        addNonEmptyError(validator.getInvalidityInfoForCourseId(courseId), errors);

        addNonEmptyError(validator.getInvalidityInfoForFeedbackSessionName(feedbackSessionName), errors);

        addNonEmptyError(validator.getInvalidityInfoForCommentGiverType(commentGiverType), errors);

        //TODO: handle the new attributes showCommentTo and showGiverNameTo

        return errors;
    }

    @Override
    public FeedbackResponseComment toEntity() {
        return new FeedbackResponseComment(courseId, feedbackSessionName, feedbackQuestionId, commentGiver,
                commentGiverType, feedbackResponseId, createdAt, commentText, giverSection, receiverSection,
                showCommentTo, showGiverNameTo, lastEditorEmail, lastEditedAt);
    }

    @Override
    public String getIdentificationString() {
        return toString();
    }

    @Override
    public String getEntityTypeAsString() {
        return "FeedbackResponseComment";
    }

    @Override
    public String getBackupIdentifier() {
        return Const.SystemParams.COURSE_BACKUP_LOG_MSG + courseId;
    }

    @Override
    public String getJsonString() {
        return JsonUtils.toJson(this, FeedbackResponseCommentAttributes.class);
    }

    @Override
    public void sanitizeForSaving() {
        this.commentText = SanitizationHelper.sanitizeForRichText(this.commentText);
    }

    @Override
    public String toString() {
        //TODO: print visibilityOptions also
        return "FeedbackResponseCommentAttributes ["
                + "feedbackResponseCommentId = " + feedbackResponseCommentId
                + ", courseId = " + courseId
                + ", feedbackSessionName = " + feedbackSessionName
                + ", feedbackQuestionId = " + feedbackQuestionId
                + ", giverEmail = " + commentGiver
                + ", feedbackResponseId = " + feedbackResponseId
                + ", commentText = " + commentText.getValue()
                + ", createdAt = " + createdAt
                + ", lastEditorEmail = " + lastEditorEmail
                + ", lastEditedAt = " + lastEditedAt + "]";
    }

    public static void sortFeedbackResponseCommentsByCreationTime(List<FeedbackResponseCommentAttributes> frcs) {
        frcs.sort(Comparator.comparing(frc -> frc.createdAt));
    }

    /**
     * A Builder for {@link FeedbackResponseCommentAttributes}.
     */
    public static class Builder {
        private static final String REQUIRED_FIELD_CANNOT_BE_NULL = "Required field cannot be null";

        private final FeedbackResponseCommentAttributes frca;

        public Builder(String courseId, String feedbackSessionName, String giverEmail, Text commentText) {
            frca = new FeedbackResponseCommentAttributes();

            validateRequiredFields(courseId, feedbackSessionName, giverEmail, commentText);

            frca.courseId = courseId;
            frca.feedbackSessionName = feedbackSessionName;
            frca.commentGiver = giverEmail;
            frca.commentText = commentText;
        }

        public Builder withFeedbackResponseId(String feedbackResponseId) {
            if (feedbackResponseId != null) {
                frca.feedbackResponseId = feedbackResponseId;
            }

            return this;
        }

        public Builder withFeedbackQuestionId(String feedbackQuestionId) {
            if (feedbackQuestionId != null) {
                frca.feedbackQuestionId = feedbackQuestionId;
            }

            return this;
        }

        public Builder withShowCommentTo(List<FeedbackParticipantType> showCommentTo) {
            frca.showCommentTo = showCommentTo == null ? new ArrayList<FeedbackParticipantType>() : showCommentTo;
            return this;
        }

        public Builder withShowGiverNameTo(List<FeedbackParticipantType> showGiverNameTo) {
            frca.showGiverNameTo = showGiverNameTo == null ? new ArrayList<FeedbackParticipantType>() : showGiverNameTo;
            return this;
        }

        public Builder withVisibilityFollowingFeedbackQuestion(Boolean visibilityFollowingFeedbackQuestion) {
            frca.isVisibilityFollowingFeedbackQuestion = visibilityFollowingFeedbackQuestion == null
                    || visibilityFollowingFeedbackQuestion; // true as default value if param is null
            return this;
        }

        public Builder withCreatedAt(Instant createdAt) {
            if (createdAt != null) {
                frca.createdAt = createdAt;
            }

            return this;
        }

        public Builder withLastEditorEmail(String lastEditorEmail) {
            frca.lastEditorEmail = lastEditorEmail == null
                    ? frca.commentGiver
                    : lastEditorEmail;
            return this;
        }

        public Builder withLastEditedAt(Instant lastEditedAt) {
            frca.lastEditedAt = lastEditedAt == null
                    ? frca.createdAt
                    : lastEditedAt;
            return this;
        }

        public Builder withFeedbackResponseCommentId(Long feedbackResponseCommentId) {
            if (feedbackResponseCommentId != null) {
                frca.feedbackResponseCommentId = feedbackResponseCommentId;
            }
            return this;
        }

        public Builder withGiverSection(String giverSection) {
            frca.giverSection = giverSection == null ? Const.DEFAULT_SECTION : giverSection;
            return this;
        }

        public Builder withReceiverSection(String receiverSection) {
            frca.receiverSection = receiverSection == null
                    ? Const.DEFAULT_SECTION
                    : receiverSection;
            return this;
        }

        public Builder withCommentGiverType(FeedbackParticipantType commentGiverType) {
            frca.commentGiverType = commentGiverType;
            return this;
        }

        public FeedbackResponseCommentAttributes build() {
            return frca;
        }

        private void validateRequiredFields(Object... objects) {
            for (Object object : objects) {
                Objects.requireNonNull(object, REQUIRED_FIELD_CANNOT_BE_NULL);
            }
        }
    }

    /**
     * Sets default visibility settings for respondent comments.
     */
    public void setVisibilitySettingsForStudentComment() {
        FeedbackParticipantType[] types = {
                FeedbackParticipantType.GIVER,
                FeedbackParticipantType.INSTRUCTORS
        };
        for (FeedbackParticipantType type : types) {
            showCommentTo.add(type);
            showGiverNameTo.add(type);
        }
    }

    public void setVisibilitySettingsForInstructorComment(String showCommentTo, String showGiverNameTo) {
        this.showCommentTo = new ArrayList<>();
        if (showCommentTo != null && !showCommentTo.isEmpty()) {
            String[] showCommentToArray = showCommentTo.split(",");
            for (String viewer : showCommentToArray) {
                this.showCommentTo.add(FeedbackParticipantType.valueOf(viewer.trim()));
            }
        }
        this.showGiverNameTo = new ArrayList<>();
        if (showGiverNameTo != null && !showGiverNameTo.isEmpty()) {
            String[] showGiverNameToArray = showGiverNameTo.split(",");
            for (String viewer : showGiverNameToArray) {
                this.showGiverNameTo.add(FeedbackParticipantType.valueOf(viewer.trim()));
            }
        }
    }
}
