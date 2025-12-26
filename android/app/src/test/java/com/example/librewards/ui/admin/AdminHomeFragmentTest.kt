package com.example.librewards.ui.admin

import android.widget.Button
import android.widget.EditText
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.utils.BaseUiTest
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class AdminHomeFragmentTest : BaseUiTest() {
    @Mock private lateinit var mockProductsRef: DatabaseReference

    @Mock private lateinit var mockStorageRef: StorageReference

    @Mock private lateinit var mockStudentRef: DatabaseReference

    @Mock private lateinit var mockStudyingRef: DatabaseReference

    @Mock private lateinit var mockRedeemingRef: DatabaseReference

    private val studentId = "someUserId"

    override fun setup() {
        super.setup()
        `when`(firebaseTestRule.mockRootRef.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(anyString())).thenReturn(mockProductsRef)
        `when`(firebaseTestRule.mockStorageRef.child(anyString())).thenReturn(mockStorageRef)

        val studentUser = User("Student", "User", "student@example.com", "Test Uni").apply {
            studying = "0"
            redeemingReward = "0"
        }
        setupUser(studentId, studentUser, mockStudentRef)

        `when`(mockStudentRef.child("studying")).thenReturn(mockStudyingRef)
        `when`(mockStudentRef.child("redeemingReward")).thenReturn(mockRedeemingRef)
    }

    @Test
    fun `toggle timer button updates user studying status`() = launchActivity<AdminActivity> { activity ->
        performAction(activity, R.id.toggleTimerButton)
        verify(mockStudentRef).child("studying")
        verify(mockStudyingRef).setValue("1")
        assertEquals(activity.getString(R.string.timer_started), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `redeem reward button updates user redeeming status`() = launchActivity<AdminActivity> { activity ->
        performAction(activity, R.id.redeemRewardButton)
        verify(mockStudentRef).child("redeemingReward")
        verify(mockRedeemingRef).setValue("1")
        assertEquals(activity.getString(R.string.reward_redeemed), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `toggle timer for non-existent student shows error toast`() =
        verifyActionForNonExistentUser(R.id.toggleTimerButton, R.string.error_starting_timer)

    @Test
    fun `redeem reward for non-existent student shows error toast`() =
        verifyActionForNonExistentUser(R.id.redeemRewardButton, R.string.error_redeeming_reward)

    private fun performAction(activity: AdminActivity, buttonId: Int, userId: String = studentId) {
        activity.findViewById<EditText>(R.id.enterQr).setText(userId)
        activity.findViewById<Button>(buttonId).performClick()
        ShadowLooper.runUiThreadTasks()
    }

    private fun verifyActionForNonExistentUser(buttonId: Int, expectedToastId: Int) {
        val nonExistentId = "unknownUser"
        setupUser(nonExistentId, null)
        launchActivity<AdminActivity> { activity ->
            performAction(activity, buttonId, nonExistentId)
            assertEquals(activity.getString(expectedToastId), ShadowToast.getTextOfLatestToast())
        }
    }

    private fun setupUser(id: String, user: User?, mockRef: DatabaseReference = Mockito.mock(DatabaseReference::class.java)) {
        val mockTask = Mockito.mock(Task::class.java) as Task<DataSnapshot>
        val mockSnapshot = Mockito.mock(DataSnapshot::class.java)

        `when`(firebaseTestRule.mockUsersRef.child(eq(id))).thenReturn(mockRef)
        `when`(mockRef.get()).thenReturn(mockTask)
        mockTask(mockTask)
        `when`(mockTask.result).thenReturn(mockSnapshot)
        `when`(mockSnapshot.getValue(User::class.java)).thenReturn(user)
    }
}
