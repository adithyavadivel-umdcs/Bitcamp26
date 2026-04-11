
package com.example.bitcamp26.data.maps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * Repository for launching map/navigation intents.
 */
public class MapsIntentRepository {

    /**
     * Opens a map app at a specific latitude/longitude pin.
     */
    public void openLocation(@NonNull Context context,
                             double latitude,
                             double longitude,
                             @NonNull String label) {
        String encodedLabel = Uri.encode(label);
        String uriString = "geo:" + latitude + "," + longitude + "?q="
                + latitude + "," + longitude + "(" + encodedLabel + ")";

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            context.startActivity(fallbackIntent);
        }
    }

    /**
     * Opens turn-by-turn directions to a destination.
     */
    public void openDirections(@NonNull Context context,
                               double destinationLat,
                               double destinationLng) {
        String uriString = "google.navigation:q=" + destinationLat + "," + destinationLng;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            String fallbackUri = "https://www.google.com/maps/dir/?api=1&destination="
                    + destinationLat + "," + destinationLng;
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri));
            context.startActivity(fallbackIntent);
        }
    }

    /**
     * Opens a search query in a map app.
     */
    public void searchPlace(@NonNull Context context,
                            @NonNull String query) {
        String encodedQuery = Uri.encode(query);
        String uriString = "geo:0,0?q=" + encodedQuery;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
            context.startActivity(fallbackIntent);
        }
    }
}
