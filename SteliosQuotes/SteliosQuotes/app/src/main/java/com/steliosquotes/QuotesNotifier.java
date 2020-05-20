package com.steliosquotes;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import androidx.core.app.NotificationCompat;

import static android.app.Notification.DEFAULT_SOUND;
import static android.app.Notification.DEFAULT_VIBRATE;

public class QuotesNotifier extends BroadcastReceiver {

    private final static String cTitleSuccess = "Stelios Quote";
    private final static String cTitleError = cTitleSuccess + "-Error";
    private final static String cNotificationChannel = "Stelios_quotes_channel";
    private static final int cNotificationId = 1;

    private class GetQuoteException extends Exception {
        GetQuoteException(String aMessage) {
            super(aMessage);
        }
    }

    @Override
    public void onReceive(Context aContext, Intent intent) {
        try {
            String quote = getQuote(aContext);
            if (quote != null && !quote.isEmpty()) {
                showNotification(aContext, cTitleSuccess, quote);
            } else
                showNotification(aContext, cTitleError, "The quote is empty");
        } catch (Exception e) {
            showNotification(aContext, cTitleError, e.getMessage());
        }
    }

    private static void showNotification(Context aContext, String aTitle, String aText) {

        NotificationManager notificationManager = (NotificationManager) aContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(cNotificationChannel, "Stelios Quotes", NotificationManager.IMPORTANCE_DEFAULT);

            // Configure the notification channel.
            notificationChannel.setDescription("Stelios notification channel");
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Notification notification = new NotificationCompat.Builder(aContext, cNotificationChannel)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(aContext.getResources(), R.mipmap.ic_launcher_round))
                .setContentTitle(aTitle)
                .setContentText(aText)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(aText))
                //this works on may old samsung, but not on my newer chinese one....
                //some suggest to do use new long[]{0}, but it didn't work neither...
                .setVibrate(null)
                .build();

        notificationManager.notify(cNotificationId, notification);

    }

    private String getQuote(Context aContext) throws GetQuoteException, IOException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(aContext);
        if (prefs == null)
            throw new GetQuoteException("Couldn't get preferences");

        String filePath = prefs.getString(aContext.getString(R.string.prefFilename), null);
        if (filePath == null)
            throw new GetQuoteException("Couldn't get configured filename");

        final Uri fileUri = Uri.parse(filePath);
        if (fileUri == null)
            throw new GetQuoteException("Failed to parse configured filename");

        List<String> quotes = readFile(aContext, fileUri);
        if (quotes != null && !quotes.isEmpty()) {
            return quotes.get(new Random().nextInt(quotes.size()));
        }

        return null;
    }

    private static List<String> readFile(Context aContext, Uri aUri) throws IOException {
        List<String> lines = new LinkedList<>();
        ContentResolver resolver = aContext.getContentResolver();
        if (resolver != null) {

            try (InputStream inputStream = resolver.openInputStream(aUri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isEmpty())
                        lines.add(line);
                }
            }
        }
        return lines;
    }
}
