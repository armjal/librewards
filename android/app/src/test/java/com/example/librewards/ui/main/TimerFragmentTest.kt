package com.example.librewards.ui.main

import android.Manifest
import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Looper
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.example.librewards.R
import com.example.librewards.utils.MainDispatcherRule
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
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], instrumentedPackages = ["androidx.loader.content"])
@ExperimentalCoroutinesApi
class TimerFragmentTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockFirebaseUser: FirebaseUser

    @Mock
    private lateinit var mockFirebaseDatabase: FirebaseDatabase

    @Mock
    private lateinit var mockDbRef: DatabaseReference

    @Mock
    private lateinit var mockUsersRef: DatabaseReference

    @Mock
    private lateinit var mockSpecificUserRef: DatabaseReference

    @Mock
    private lateinit var mockStudyingRef: DatabaseReference

    @Mock
    private lateinit var mockPointsRef: DatabaseReference

    @Mock
    private lateinit var mockStudyingSnapshot: DataSnapshot

    @Mock
    private lateinit var mockPointsSnapshot: DataSnapshot

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

    private lateinit var mockedAuth: MockedStatic<FirebaseAuth>
    private lateinit var mockedDb: MockedStatic<FirebaseDatabase>
    private lateinit var mockedLocationServices: MockedStatic<LocationServices>
    private lateinit var mockedApp: MockedStatic<FirebaseApp>
    private lateinit var mockedCameraUpdateFactory: MockedStatic<CameraUpdateFactory>

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Mock Firebase
        mockedAuth = mockStatic(FirebaseAuth::class.java)
        mockedAuth.`when`<FirebaseAuth> { FirebaseAuth.getInstance() }.thenReturn(mockFirebaseAuth)
        `when`(mockFirebaseAuth.currentUser).thenReturn(mockFirebaseUser)
        `when`(mockFirebaseUser.email).thenReturn("test@example.com")

        mockedDb = mockStatic(FirebaseDatabase::class.java)
        mockedDb.`when`<FirebaseDatabase> { FirebaseDatabase.getInstance() }.thenReturn(mockFirebaseDatabase)
        `when`(mockFirebaseDatabase.reference).thenReturn(mockDbRef)

        // Mock DB Structure
        `when`(mockDbRef.child("users")).thenReturn(mockUsersRef)
        `when`(mockDbRef.child("products"))
            .thenReturn(Mockito.mock(DatabaseReference::class.java))
        `when`(mockUsersRef.child(ArgumentMatchers.anyString())).thenReturn(mockSpecificUserRef)
        `when`(mockSpecificUserRef.child("studying")).thenReturn(mockStudyingRef)
        `when`(mockSpecificUserRef.child("points")).thenReturn(mockPointsRef)
        `when`(mockSpecificUserRef.child("redeemingReward"))
            .thenReturn(Mockito.mock(DatabaseReference::class.java))

        // Mock LocationServices for MapsViewModel
        mockedLocationServices = mockStatic(LocationServices::class.java)
        mockedLocationServices.`when`<FusedLocationProviderClient> {
            LocationServices.getFusedLocationProviderClient(ArgumentMatchers.any(Activity::class.java))
        }.thenReturn(mockFusedLocationClient)

        mockedApp = mockStatic(FirebaseApp::class.java)
        mockedApp.`when`<FirebaseApp> { FirebaseApp.getInstance() }.thenReturn(
            mock(
                FirebaseApp::class.java,
            ),
        )

        mockedCameraUpdateFactory = mockStatic(CameraUpdateFactory::class.java)
        mockedCameraUpdateFactory.`when`<CameraUpdate> {
            CameraUpdateFactory.newLatLngZoom(any(), any())
        }.thenReturn(mock(CameraUpdate::class.java))
    }

    @After
    fun tearDown() {
        mockedAuth.close()
        mockedDb.close()
        mockedLocationServices.close()
        mockedApp.close()
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
                    // 1. Setup initial points
                    val pointsCaptor = argumentCaptor<ValueEventListener>()
                    verify(mockPointsRef, Mockito.atLeastOnce()).addValueEventListener(pointsCaptor.capture())

                    `when`(mockPointsSnapshot.value).thenReturn(params.initialPoints.toString())
                    pointsCaptor.firstValue.onDataChange(mockPointsSnapshot)
                    ShadowLooper.runUiThreadTasks()

                    // 2. Start timer (studying = "1")
                    val studyingCaptor = argumentCaptor<ValueEventListener>()
                    verify(mockStudyingRef, Mockito.atLeastOnce()).addValueEventListener(studyingCaptor.capture())

                    `when`(mockStudyingSnapshot.value).thenReturn("1")
                    studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
                    ShadowLooper.runUiThreadTasks()

                    // 3. Advance time
                    ShadowSystemClock.advanceBy(Duration.ofSeconds(params.durationSeconds))

                    // 4. Stop timer (studying = "0")
                    `when`(mockStudyingSnapshot.value).thenReturn("0")
                    studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
                    ShadowLooper.runUiThreadTasks()

                    // 5. Verify database update
                    verify(mockPointsRef).setValue(params.expectedTotalPoints.toString())

                    // 6. Verify Popup UI
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

                    // 7. Verify Points TextView update
                    `when`(mockPointsSnapshot.value).thenReturn(params.expectedTotalPoints.toString())
                    pointsCaptor.firstValue.onDataChange(mockPointsSnapshot)
                    ShadowLooper.runUiThreadTasks()

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
        // Grant permissions via Robolectric shadow
        val app = getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)
        shadowApp.grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity {
                // Verify setupMap() -> mapsViewModel.listenToLocationChanges() -> fusedLocationClient.requestLocationUpdates
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
                // Verify setupMap() was NOT called, so requestLocationUpdates should NOT happen
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

                // 1. Setup initial points
                val pointsCaptor = argumentCaptor<ValueEventListener>()
                verify(mockPointsRef, Mockito.atLeastOnce()).addValueEventListener(pointsCaptor.capture())
                `when`(mockPointsSnapshot.value).thenReturn("0")
                pointsCaptor.firstValue.onDataChange(mockPointsSnapshot)
                ShadowLooper.runUiThreadTasks()

                // 2. Simulate initial location (the "zone")
                `when`(mockLocation1.latitude).thenReturn(10.0)
                `when`(mockLocation1.longitude).thenReturn(10.0)
                `when`(mockLocation1.distanceTo(mockLocation1)).thenReturn(0f)
                callbackCaptor.firstValue.onLocationResult(LocationResult.create(listOf(mockLocation1)))

                // 3. Start timer
                val studyingCaptor = argumentCaptor<ValueEventListener>()
                verify(mockStudyingRef, Mockito.atLeastOnce()).addValueEventListener(studyingCaptor.capture())
                `when`(mockStudyingSnapshot.value).thenReturn("1")
                studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
                ShadowLooper.runUiThreadTasks()

                // Verify circle color is blue (inside zone)
                verify(mockCircle).fillColor = Color.parseColor("#4d318ce7")

                // 4. Spend time at the library to be eligible to gain points
                ShadowSystemClock.advanceBy(Duration.ofSeconds(65))

                // 5. Simulate moving out of zone (> 40m)
                `when`(mockLocation2.latitude).thenReturn(10.0005)
                `when`(mockLocation2.longitude).thenReturn(10.0005)
                `when`(mockLocation1.distanceTo(mockLocation2)).thenReturn(50f)
                callbackCaptor.firstValue.onLocationResult(LocationResult.create(listOf(mockLocation2)))
                ShadowLooper.runUiThreadTasks()

                // Verify circle color is red (outside zone)
                verify(mockCircle).fillColor = Color.parseColor("#4dff0000")

                // 6. Verify timer stopped (studying = "2" for reset)
                verify(mockStudyingRef).setValue("2")

                // 7. Verify points earned are 0 and textView is still "0"
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

                // Start timer
                val studyingCaptor = argumentCaptor<ValueEventListener>()
                verify(mockStudyingRef, Mockito.atLeastOnce()).addValueEventListener(studyingCaptor.capture())
                `when`(mockStudyingSnapshot.value).thenReturn("1")
                studyingCaptor.firstValue.onDataChange(mockStudyingSnapshot)
                ShadowLooper.runUiThreadTasks()
                val stopwatch = activity.findViewById<Chronometer>(R.id.stopwatch)

                // Advance time past 24 hours
                val twentyFourHoursInMillis = Duration.ofHours(24).toMillis() + 1000
                stopwatch.base = ShadowSystemClock.currentTimeMillis() - twentyFourHoursInMillis

                // Verify Popup
                val latestDialog = ShadowDialog.getLatestDialog()
                assertNotNull("Popup should be shown", latestDialog)
                val textView = latestDialog.findViewById<TextView>(R.id.popupText)
                assertEquals(
                    activity.getString(R.string.no_stop_code_entered),
                    textView.text.toString(),
                )

                // Verify Chronometer listener removed (indicating resetTimerState called)]
                assertNull(stopwatch.onChronometerTickListener)
            }
        }
    }
}
