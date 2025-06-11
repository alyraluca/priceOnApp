package com.example.myapplication3.priceon.activities;

import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.Mockito.*;
import android.Manifest;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.myapplication3.priceon.R;
import com.example.myapplication3.priceon.data.model.Product;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLocationManager;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class HomActivityTests {
    private ActivityController<HomeActivity> controller;
    private HomeActivity activity;

    @Mock FirebaseFirestore mockFirestore;
    @Mock CollectionReference mockUsersCollection;
    @Mock CollectionReference mockProductsCollection;
    @Mock Query mockProductQuery;
    @Mock QuerySnapshot mockQuerySnapshot;
    @Mock DocumentSnapshot mockDoc;

    @Before
    public void setUp() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        MockitoAnnotations.initMocks(this);

        // Create activity and spy
        controller = Robolectric.buildActivity(HomeActivity.class);
        // Asignar tema AppCompat para evitar No theme error
        controller.get().setTheme(android.R.style.Theme_Material_Light);
        activity = spy(controller.create().start().resume().get());

        // Inject mock Firestore and uid directly
        activity.db = mockFirestore;
        activity.uid = "testUid";

        // Stub Firestore collections
        when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
        when(mockFirestore.collection("products")).thenReturn(mockProductsCollection);

        // Stub search query
        when(mockProductsCollection.orderBy("name")).thenReturn(mockProductQuery);
        when(mockProductQuery.startAt("apple")).thenReturn(mockProductQuery);
        when(mockProductQuery.endAt("apple\uf8ff")).thenReturn(mockProductQuery);
        when(mockProductQuery.get()).thenReturn(Tasks.forResult(mockQuerySnapshot));

        // Stub document snapshot
        Product stubProduct = new Product();
        stubProduct.setId("p1");
        stubProduct.setName("Apple");
        when(mockDoc.toObject(Product.class)).thenReturn(stubProduct);
        when(mockQuerySnapshot.getDocuments()).thenReturn(Collections.singletonList(mockDoc));

        // Override async loaders to run immediately
        doAnswer(invocation -> { Runnable cb = invocation.getArgument(1); cb.run(); return null; })
                .when(activity).loadProductBrand(any(Product.class), any(Runnable.class));
        doAnswer(invocation -> { Runnable cb = invocation.getArgument(1); cb.run(); return null; })
                .when(activity).loadPricesAndSupermarkets(any(Product.class), any(Runnable.class));
    }

    @Test
    public void whenNoUser_historyViewsHidden() {
        // Simulate no user by clearing uid
        activity.uid = null;
        controller = Robolectric.buildActivity(HomeActivity.class);
        activity = controller.create().start().resume().get();

        assertEquals("", ((TextView) activity.findViewById(R.id.labelSearchHistory)).getText().toString());
        assertEquals(View.GONE, activity.findViewById(R.id.historyRecyclerView).getVisibility());
    }

    @Test
    public void editorAction_withText_performsSearchAndUpdatesList() {
        EditText search = activity.findViewById(R.id.searchEditText);
        search.setText("apple");
        search.onEditorAction(EditorInfo.IME_ACTION_SEARCH);

        // Verify UI hidden
        assertEquals(View.GONE, activity.findViewById(R.id.labelSearchHistory).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.nearbyCard).getVisibility());
        // Verify productList updated
        assertEquals(1, activity.productList.size());
        assertEquals("p1", activity.productList.get(0).getId());
    }

    @Test
    public void requestPermissionDenied_hidesNearbyCard() {
        activity.onRequestPermissionsResult(42,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                new int[]{android.content.pm.PackageManager.PERMISSION_DENIED});
        assertEquals(View.GONE, activity.findViewById(R.id.nearbyCard).getVisibility());
    }

    @Test
    public void setupNearby_noLocation_hidesNearbyCard() {
        ShadowApplication.getInstance().grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        ShadowLocationManager shadowLm = shadowOf(
                (android.location.LocationManager)
                        activity.getSystemService(android.content.Context.LOCATION_SERVICE)
        );
        shadowLm.setLastKnownLocation(android.location.LocationManager.GPS_PROVIDER, null);
        shadowLm.setLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER, null);

        activity.setupNearbySupermarkets();

        assertEquals(View.GONE, activity.findViewById(R.id.nearbyCard).getVisibility());
    }
}
