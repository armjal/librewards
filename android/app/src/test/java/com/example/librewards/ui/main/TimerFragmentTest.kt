package com.example.librewards.ui.main

import android.Manifest
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.librewards.R
import com.example.librewards.utils.BaseUiTest
import com.example.librewards.utils.TestUtils
import com.example.librewards.utils.generateIdFromKey
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

class TimerFragmentTest : BaseUiTest() {
    @Mock
    private lateinit var mockStudyingRef: DatabaseReference

    @Mock
    private lateinit var mockStudyingSnapshot: DataSnapshot

    @Mock
    private lateinit var mockFusedLocationClient: FusedLocationProviderClient

    @Mock
    private lateinit var mockLocation1: Location

    @Mock
    private lateinit var mockLocation2: Location

    @Mock
    private lateinit var mockGoogleMap: GoogleMap

    @Mock
    private lateinit var mockCircle: Circle

    @Mock
    private lateinit var mockMarker: Marker

    private lateinit var mockedLocationServices: MockedStatic<LocationServices>
    private lateinit var mockedCameraUpdateFactory: MockedStatic<CameraUpdateFactory>

    override fun setup() {
        super.setup()
        setupFirebase()

        mockedLocationServices = mockStatic(LocationServices::class.java)
        mockedLocationServices.`when`<FusedLocationProviderClient> {
            LocationServices.getFusedLocationProviderClient(ArgumentMatchers.any(Activity::class.java))
        }.thenReturn(mockFusedLocationClient)

        mockedCameraUpdateFactory = mockStatic(CameraUpdateFactory::class.java)
        mockedCameraUpdateFactory.`when`<CameraUpdate> {
            CameraUpdateFactory.newLatLngZoom(any(), any())
        }.thenReturn(mock(CameraUpdate::class.java))
    }

    private fun setupFirebase() {
        val mockProductsRef = Mockito.mock(DatabaseReference::class.java)
        `when`(firebaseTestRule.mockRootRef.child("products"))
            .thenReturn(mockProductsRef)
        `when`(mockProductsRef.child(any())).thenReturn(mockProductsRef)

        `when`(firebaseTestRule.mockSpecificUserRef.child("studying")).thenReturn(mockStudyingRef)
        `when`(firebaseTestRule.mockSpecificUserRef.child("redeemingReward"))
            .thenReturn(Mockito.mock(DatabaseReference::class.java))
    }

    @After
    fun tearDown() {
        mockedLocationServices.close()
        mockedCameraUpdateFactory.close()
    }

    @Test
    fun `fragment displays correct UI elements`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val stopwatch = activity.findViewById<Chronometer>(R.id.stopwatch)
                val slidingPanel = activity.findViewById<SlidingUpPanelLayout>(R.id.slidingPanel)
                val qrCode = activity.findViewById<ImageView>(R.id.qrCode)
                val userPoints = activity.findViewById<TextView>(R.id.usersPoints)

                assertNotNull("Stopwatch should be visible", stopwatch)
                assertNotNull("SlidingPanel should be visible", slidingPanel)
                assertNotNull("QRCode should be visible", qrCode)
                assertNotNull("UserPoints should be visible", userPoints)
            }
        }
    }

    @Test
    fun `timer starts, stops, and updates points with varying parameters`() {
        data class TestParams(
            val durationSeconds: Long,
            val initialPoints: Int,
            val expectedEarnedPoints: Int,
            val expectedTotalPoints: Int,
            val expectedMinutesText: String,
        )

        val testCases =
            listOf(
                TestParams(65, 50, 75, 125, "minute"),
                TestParams(125, 0, 125, 125, "minutes"),
            )

        testCases.forEach { params ->
            ShadowDialog.reset()
            Mockito.clearInvocations(mockPointsRef, mockStudyingRef)

            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    setupPoints(params.initialPoints.toString())

                    // Mock location call for startTimer which might trigger setChosenLocation
                    val mockTask = Mockito.mock(Task::class.java) as Task<Location>
                    `when`(mockFusedLocationClient.getCurrentLocation(anyInt(), ArgumentMatchers.any()))
                        .thenReturn(mockTask)
                    TestUtils.mockTask(mockTask)

                    startTimer()

                    ShadowSystemClock.advanceBy(Duration.ofSeconds(params.durationSeconds))

                    val studyingCaptor = argumentCaptor<ValueEventListener>()
                    verify(mockStudyingRef, Mockito.atLeastOnce()).addValueEventListener(studyingCaptor.capture())
                    `when`(mockStudyingSnapshot.value).thenReturn("0")
                    studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
                    ShadowLooper.runUiThreadTasks()

                    verify(mockPointsRef).setValue(params.expectedTotalPoints.toString())

                    val latestDialog = ShadowDialog.getLatestDialog()
                    Assert.assertNotNull(latestDialog)
                    val textView = latestDialog.findViewById<TextView>(R.id.popupText)
                    val minutesSpent = params.durationSeconds / 60
                    val expectedPopupText =
                        "Well done, you spent $minutesSpent ${params.expectedMinutesText} " +
                            "at the library and have earned ${params.expectedEarnedPoints} points!\n" +
                            "Your new points balance is: ${params.expectedTotalPoints}"
                    Assert.assertEquals(expectedPopupText, textView!!.text.toString())
                    latestDialog.findViewById<ImageView>(R.id.closeBtn).performClick()

                    setupPoints(params.expectedTotalPoints.toString())

                    assertEquals(
                        params.expectedTotalPoints.toString(),
                        activity.findViewById<TextView>(R.id.usersPoints).text.toString(),
                    )
                }
            }
        }
    }

    @Test
    fun `sets up map when permission is granted`() {
        val app = getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                verify(mockFusedLocationClient).requestLocationUpdates(
                    any<LocationRequest>(),
                    any<LocationCallback>(),
                    any<Looper>(),
                )
            }
        }
    }

    @Test
    fun `does not set up map when permission is denied`() {
        val app = getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.denyPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                verify(mockFusedLocationClient, Mockito.never()).requestLocationUpdates(
                    any<LocationRequest>(),
                    any<LocationCallback>(),
                    any<Looper>(),
                )
            }
        }
    }

    @Test
    fun `timer stops and points are zero if student moves out of zone`() {
        Shadows.shadowOf(getApplicationContext<Application>()).grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                (activity.supportFragmentManager.fragments.firstOrNull { it is TimerFragment } as? TimerFragment)?.onMapReady(mockGoogleMap)
                `when`(mockGoogleMap.addCircle(any())).thenReturn(mockCircle)
                `when`(mockGoogleMap.addMarker(any())).thenReturn(mockMarker)

                val callbackCaptor = argumentCaptor<LocationCallback>()
                verify(mockFusedLocationClient).requestLocationUpdates(any<LocationRequest>(), callbackCaptor.capture(), any<Looper>())

                setupPoints("0")

                `when`(mockLocation1.latitude).thenReturn(10.0)
                `when`(mockLocation1.longitude).thenReturn(10.0)
                `when`(mockLocation1.distanceTo(mockLocation1)).thenReturn(0f)
                callbackCaptor.firstValue.onLocationResult(LocationResult.create(listOf(mockLocation1)))

                val mockTask = Mockito.mock(Task::class.java) as Task<Location>
                `when`(mockFusedLocationClient.getCurrentLocation(anyInt(), ArgumentMatchers.any()))
                    .thenReturn(mockTask)

                `when`(mockTask.result).thenReturn(mockLocation1)
                TestUtils.mockTask(mockTask)

                startTimer()

                ShadowLooper.runUiThreadTasks()
                val blue = "#4d318ce7"
                verify(mockCircle).fillColor = Color.parseColor(blue)

                ShadowSystemClock.advanceBy(Duration.ofSeconds(65))

                `when`(mockLocation2.latitude).thenReturn(11.0000)
                `when`(mockLocation2.longitude).thenReturn(11.0000)
                `when`(mockLocation1.distanceTo(mockLocation2)).thenReturn(70f)
                callbackCaptor.firstValue.onLocationResult(LocationResult.create(listOf(mockLocation2)))
                ShadowLooper.runUiThreadTasks()

                verify(mockCircle).remove()

                verify(mockStudyingRef).setValue("2")

                verify(mockPointsRef, Mockito.never()).setValue(any())
                assertEquals(
                    "0", activity.findViewById<TextView>(R.id.usersPoints).text.toString(),
                )
            }
        }
    }

    @Test
    fun `timer resets if runs for 24 hours`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                ShadowDialog.reset()

                val mockTask = Mockito.mock(Task::class.java) as Task<Location>
                `when`(mockFusedLocationClient.getCurrentLocation(anyInt(), ArgumentMatchers.any()))
                    .thenReturn(mockTask)
                TestUtils.mockTask(mockTask)

                startTimer()

                val stopwatch = activity.findViewById<Chronometer>(R.id.stopwatch)

                val twentyFourHoursInMillis = 86400000L
                stopwatch.base = SystemClock.elapsedRealtime() - twentyFourHoursInMillis

                ShadowSystemClock.advanceBy(Duration.ofSeconds(2))

                val latestDialog = ShadowDialog.getLatestDialog()
                assertNotNull("Popup should be shown", latestDialog)
                val textView = latestDialog.findViewById<TextView>(R.id.popupText)
                assertEquals(
                    activity.getString(R.string.no_stop_code_entered),
                    textView.text.toString(),
                )

                assertNull(stopwatch.onChronometerTickListener)
            }
        }
    }

    @Test
    fun `slider panel contains the correct qr code number`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val qrCodeNumber = activity.findViewById<TextView>(R.id.qrCodeNumber)
                val expectedNumber = generateIdFromKey("test@example.com")
                assertEquals(expectedNumber, qrCodeNumber.text.toString())
            }
        }
    }

    private fun startTimer() {
        val studyingCaptor = argumentCaptor<ValueEventListener>()
        verify(mockStudyingRef, Mockito.atLeastOnce()).addValueEventListener(studyingCaptor.capture())
        `when`(mockStudyingSnapshot.value).thenReturn("1")
        studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
        ShadowLooper.runUiThreadTasks()
    }
}
