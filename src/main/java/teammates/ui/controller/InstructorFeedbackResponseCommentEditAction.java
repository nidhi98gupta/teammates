package teammates.ui.controller;

import java.time.Instant;
import java.util.ArrayList;

import com.google.appengine.api.datastore.Text;

import teammates.common.datatransfer.FeedbackParticipantType;
import teammates.common.datatransfer.attributes.FeedbackResponseAttributes;
import teammates.common.datatransfer.attributes.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.attributes.FeedbackSessionAttributes;
import teammates.common.datatransfer.attributes.InstructorAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.Assumption;
import teammates.common.util.Const;
import teammates.ui.pagedata.FeedbackResponseCommentAjaxPageData;

/**
 * Action: Edit {@link FeedbackResponseCommentAttributes}.
 */
public class InstructorFeedbackResponseCommentEditAction extends Action {

    @Override
    protected ActionResult execute() throws EntityDoesNotExistException {
        String courseId = getRequestParamValue(Const.ParamsNames.COURSE_ID);
        Assumption.assertPostParamNotNull(Const.ParamsNames.COURSE_ID, courseId);
        String feedbackSessionName = getRequestParamValue(Const.ParamsNames.FEEDBACK_SESSION_NAME);
        Assumption.assertPostParamNotNull(Const.ParamsNames.FEEDBACK_SESSION_NAME, feedbackSessionName);
        String feedbackResponseId = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_ID);
        Assumption.assertPostParamNotNull(Const.ParamsNames.FEEDBACK_RESPONSE_ID, feedbackResponseId);
        String feedbackResponseCommentId = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID);
        Assumption.assertPostParamNotNull(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_ID, feedbackResponseCommentId);

        InstructorAttributes instructor = logic.getInstructorForGoogleId(courseId, account.googleId);
        FeedbackSessionAttributes session = logic.getFeedbackSession(feedbackSessionName, courseId);
        FeedbackResponseAttributes response = logic.getFeedbackResponse(feedbackResponseId);
        Assumption.assertNotNull(response);

        FeedbackResponseCommentAttributes frc =
                logic.getFeedbackResponseComment(Long.parseLong(feedbackResponseCommentId));
        Assumption.assertNotNull("FeedbackResponseComment should not be null", frc);
        verifyAccessibleForInstructorToFeedbackResponseComment(
                feedbackResponseCommentId, instructor, session, response);

        FeedbackResponseCommentAjaxPageData data =
                new FeedbackResponseCommentAjaxPageData(account, sessionToken);

        //Edit comment text
        String commentText = getRequestParamValue(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT);
        Assumption.assertPostParamNotNull(Const.ParamsNames.FEEDBACK_RESPONSE_COMMENT_TEXT, commentText);
        if (commentText.trim().isEmpty()) {
            data.errorMessage = Const.StatusMessages.FEEDBACK_RESPONSE_COMMENT_EMPTY;
            data.isError = true;
            return createAjaxResult(data);
        }

        FeedbackResponseCommentAttributes feedbackResponseComment = FeedbackResponseCommentAttributes
                .builder(courseId, feedbackSessionName, instructor.email, new Text(commentText))
                .withCreatedAt(Instant.now())
                .withGiverSection(response.giverSection)
                .withReceiverSection(response.recipientSection)
                .withCommentFromFeedbackParticipant(false)
                .withCommentGiverType(FeedbackParticipantType.INSTRUCTORS)
                .build();
        feedbackResponseComment.setId(Long.parseLong(feedbackResponseCommentId));

        //Edit visibility settings
        String showCommentTo = getRequestParamValue(Const.ParamsNames.RESPONSE_COMMENTS_SHOWCOMMENTSTO);
        String showGiverNameTo = getRequestParamValue(Const.ParamsNames.RESPONSE_COMMENTS_SHOWGIVERTO);
        feedbackResponseComment.showCommentTo = new ArrayList<>();
        if (showCommentTo != null && !showCommentTo.isEmpty()) {
            String[] showCommentToArray = showCommentTo.split(",");
            for (String viewer : showCommentToArray) {
                feedbackResponseComment.showCommentTo.add(FeedbackParticipantType.valueOf(viewer.trim()));
            }
        }
        feedbackResponseComment.showGiverNameTo = new ArrayList<>();
        if (showGiverNameTo != null && !showGiverNameTo.isEmpty()) {
            String[] showGiverNameToArray = showGiverNameTo.split(",");
            for (String viewer : showGiverNameToArray) {
                feedbackResponseComment.showGiverNameTo.add(FeedbackParticipantType.valueOf(viewer.trim()));
            }
        }

        FeedbackResponseCommentAttributes updatedComment = null;
        try {
            updatedComment = logic.updateFeedbackResponseComment(feedbackResponseComment);
            //TODO: move putDocument to task queue
            logic.putDocument(updatedComment);
        } catch (InvalidParametersException e) {
            setStatusForException(e);
            data.errorMessage = e.getMessage();
            data.isError = true;
        }

        if (!data.isError) {
            statusToAdmin += "InstructorFeedbackResponseCommentEditAction:<br>"
                           + "Editing feedback response comment: " + feedbackResponseComment.getId() + "<br>"
                           + "in course/feedback session: " + feedbackResponseComment.courseId + "/"
                           + feedbackResponseComment.feedbackSessionName + "<br>"
                           + "by: " + feedbackResponseComment.commentGiver + "<br>"
                           + "comment text: " + feedbackResponseComment.commentText.getValue();

            String commentGiverName = logic.getInstructorForEmail(courseId, frc.commentGiver).name;
            String commentEditorName = instructor.name;

            // createdAt and lastEditedAt fields in updatedComment as well as sessionTimeZone
            // are required to generate timestamps in editedCommentDetails
            data.comment = updatedComment;
            data.sessionTimeZone = session.getTimeZone();

            data.editedCommentDetails = data.createEditedCommentDetails(commentGiverName, commentEditorName);
        }

        return createAjaxResult(data);
    }

    private void verifyAccessibleForInstructorToFeedbackResponseComment(
            String feedbackResponseCommentId, InstructorAttributes instructor,
            FeedbackSessionAttributes session, FeedbackResponseAttributes response) {
        FeedbackResponseCommentAttributes frc =
                logic.getFeedbackResponseComment(Long.parseLong(feedbackResponseCommentId));
        if (frc == null) {
            return;
        }
        if (instructor != null && frc.commentGiver.equals(instructor.email)) { // giver, allowed by default
            return;
        }
        gateKeeper.verifyAccessible(instructor, session, false, response.giverSection,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
        gateKeeper.verifyAccessible(instructor, session, false, response.recipientSection,
                Const.ParamsNames.INSTRUCTOR_PERMISSION_MODIFY_SESSION_COMMENT_IN_SECTIONS);
    }

}
