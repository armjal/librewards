package com.example.librewards.ui.admin

import android.Manifest
import android.widget.Button
import android.widget.EditText
import com.example.librewards.R
import com.example.librewards.data.models.User
import com.example.librewards.utils.BaseUiTest
import com.example.librewards.utils.generateIdFromKey
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast

class AdminHomeFragmentTest : BaseUiTest() {
    @Mock private lateinit var mockProductsRef: DatabaseReference

    @Mock private lateinit var mockStorageRef: StorageReference

    @Mock private lateinit var mockStudentRef: DatabaseReference

    @Mock private lateinit var mockStudyingRef: DatabaseReference

    @Mock private lateinit var mockRedeemingRef: DatabaseReference

    @Mock private lateinit var mockScanner: GmsBarcodeScanner

    @Mock private lateinit var mockBarcodeTask: Task<Barcode>

    private lateinit var mockedGmsBarcodeScanning: MockedStatic<GmsBarcodeScanning>

    private val studentId = "someUserId"

    override fun setup() {
        super.setup()
        setupFirebaseMocks()
        setupDefaultUsers()
        setupScannerMocks()
    }

    @After
    fun tearDown() {
        mockedGmsBarcodeScanning.close()
    }

    @Test
    fun `toggle timer button updates user studying status`() = launchActivity<AdminActivity> { activity ->
        performAction(activity, R.id.toggleTimerButton)
        verify(mockStudentRef).child("studying")
        verify(mockStudyingRef).setValue("1")
        assertToast(activity, R.string.timer_started)
    }

    @Test
    fun `toggle timer button stops timer if already started`() {
        val id = "activeStudent"
        setupActiveStudent(id)

        launchActivity<AdminActivity> { activity ->
            performAction(activity, R.id.toggleTimerButton, id)
            verify(mockStudyingRef).setValue("0")
            assertToast(activity, R.string.timer_stopped)
        }
    }

    @Test
    fun `redeem reward button updates user redeeming status`() = launchActivity<AdminActivity> { activity ->
        performAction(activity, R.id.redeemRewardButton)
        verify(mockStudentRef).child("redeemingReward")
        verify(mockRedeemingRef).setValue("1")
        assertToast(activity, R.string.reward_redeemed)
    }

    @Test
    fun `redeem reward for unprepared student shows error toast`() {
        val id = "unpreparedUser"
        setupUser(id, User("S", "U", "e", "U").apply { redeemingReward = "1" })

        launchActivity<AdminActivity> { activity ->
            performAction(activity, R.id.redeemRewardButton, id)
            assertToast(activity, R.string.student_not_prepared)
        }
    }

    @Test
    fun `toggle timer for non-existent student shows error toast`() =
        verifyActionForNonExistentUser(R.id.toggleTimerButton, R.string.error_starting_timer)

    @Test
    fun `redeem reward for non-existent student shows error toast`() =
        verifyActionForNonExistentUser(R.id.redeemRewardButton, R.string.error_redeeming_reward)

    @Test
    fun `scan timer with denied camera permission requests permission`() = launchActivity<AdminActivity> {
        denyCameraPermission()

        performAction(it, R.id.scanTimerButton)

        val nextStartedActivity = Shadows.shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivityForResult
        assertEquals(
            "android.content.pm.action.REQUEST_PERMISSIONS",
            nextStartedActivity.intent.action,
        )
    }

    @Test
    fun `scan timer button starts scanner when camera permission is granted`() = launchActivity<AdminActivity> { activity ->
        grantCameraPermission()
        performAction(activity, R.id.scanTimerButton)
        verify(mockScanner).startScan()
    }

    @Test
    fun `scan reward button starts scanner when camera permission is granted`() = launchActivity<AdminActivity> { activity ->
        grantCameraPermission()
        performAction(activity, R.id.scanRewardButton)
        verify(mockScanner).startScan()
    }

    private fun setupFirebaseMocks() {
        `when`(firebaseTestRule.mockRootRef.child("products")).thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(anyString())).thenReturn(mockProductsRef)
        `when`(firebaseTestRule.mockStorageRef.child(anyString())).thenReturn(mockStorageRef)
    }

    private fun setupDefaultUsers() {
        setupUser(generateIdFromKey("test@example.com"), User("Admin", "User", "test@example.com", "Test Uni"))

        val studentUser = User("Student", "User", "student@example.com", "Test Uni").apply {
            studying = "0"
            redeemingReward = "0"
        }
        setupUser(studentId, studentUser, mockStudentRef)

        `when`(mockStudentRef.child("studying")).thenReturn(mockStudyingRef)
        `when`(mockStudentRef.child("redeemingReward")).thenReturn(mockRedeemingRef)
    }

    private fun setupScannerMocks() {
        mockedGmsBarcodeScanning = Mockito.mockStatic(GmsBarcodeScanning::class.java)
        mockedGmsBarcodeScanning.`when`<GmsBarcodeScanner> { GmsBarcodeScanning.getClient(any()) }.thenReturn(mockScanner)
        mockedGmsBarcodeScanning.`when`<GmsBarcodeScanner> { GmsBarcodeScanning.getClient(any(), any()) }.thenReturn(mockScanner)

        `when`(mockScanner.startScan()).thenReturn(mockBarcodeTask)
        mockTask(mockBarcodeTask)
    }

    private fun setupActiveStudent(id: String) {
        setupUser(
            id,
            User("Simon", "Lastly", "email@example.com", "Uni").apply {
                studying = "1"
            },
            mockStudentRef,
        )
        `when`(mockStudentRef.child("studying")).thenReturn(mockStudyingRef)
    }

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
            assertToast(activity, expectedToastId)
        }
    }

    private fun assertToast(activity: AdminActivity, messageResId: Int) {
        assertEquals(activity.getString(messageResId), ShadowToast.getTextOfLatestToast())
    }

    private fun grantCameraPermission() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)
    }

    private fun denyCameraPermission() {
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(Manifest.permission.CAMERA)
    }
}
