package nl.whitedove.nfcwageningen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import nl.whitedove.nfcwageningen.LogBoek.StatusLogBoek;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    SharedPreferences preferences;

    private static LogBoek logBoek = new LogBoek();
    private NfcAdapter nfcAdapter;
    private static final String NFCWageningen = "(N)FC Wageningen";
    private static final String Gevonden = "gevonden.txt";
    private static final int PENDING_INTENT_TECH_DISCOVERED = 1;
    private static MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisatie

        logBoek.setStatus(StatusLogBoek.Ok);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String naam = preferences.getString("Gebruikersnaam", "").trim();

        if (naam.isEmpty()) {
            instellingen();
        }

        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button1);

        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                GevondenMetGeluidje();
            }
        });

        if (AlGevonden()) {
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.INVISIBLE);
        }

        Checks();

        // Resolve the intent that started us:
        resolveIntent(this.getIntent(), false);

        ToonLogboek();
    }

    @Override
    public void onResume() {
        super.onResume();

        Checks();
        ToonLogboek();

        // Retrieve an instance of the NfcAdapter ("connection" to the NFC
        // system service):
        NfcManager nfcManager = (NfcManager) this
                .getSystemService(Context.NFC_SERVICE);
        if (nfcManager != null) {
            nfcAdapter = nfcManager.getDefaultAdapter();
        }

        if (nfcAdapter != null) {
            // Create a PendingIntent to handle discovery of Ndef and
            // NdefFormatable tags:
            PendingIntent pi = createPendingResult(
                    PENDING_INTENT_TECH_DISCOVERED, new Intent(), 0);

            if (pi != null) {
                try {
                    // Enable foreground dispatch for Ndef and NdefFormatable
                    // tags:
                    nfcAdapter
                            .enableForegroundDispatch(
                                    this,
                                    pi,
                                    new IntentFilter[]{new IntentFilter(
                                            NfcAdapter.ACTION_TECH_DISCOVERED)},
                                    new String[][]{new String[]{"android.nfc.tech.Ndef"}});
                } catch (NullPointerException e) {
                    // Drop NullPointerException that is sometimes thrown
                    // when NFC service crashed
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (nfcAdapter != null) {
            try {
                // Disable foreground dispatch:
                nfcAdapter.disableForegroundDispatch(this);
            } catch (NullPointerException e) {
                // Drop NullPointerException that is sometimes thrown
                // when NFC service crashed
            }
        }
    }

    @Override
    public void onNewIntent(Intent data) {
        // Resolve the intent that re-invoked us:
        resolveIntent(data, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PENDING_INTENT_TECH_DISCOVERED:
                // Resolve the foreground dispatch intent:
                resolveIntent(data, true);
                break;
        }
    }

    private void instellingen() {
        Intent i = new Intent(MainActivity.this, MyPrefsActivity.class);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        instellingen();
        logBoek.setStatus(StatusLogBoek.Ok);
        return true;
    }

    private void ToonLogboek() {

        SorteerLogBoek();

        // Zet de FTF etc
        setFtf(logBoek.getLogLijst());

        final TextView tv1 = (TextView) findViewById(R.id.textView1);
        tv1.setText(String.format(getResources().getString(R.string.LogCache), logBoek.getCacheNaam()));

        final TextView tv2 = (TextView) findViewById(R.id.textView2);
        tv2.setText(String.format(getResources().getString(R.string.FreeBytes), logBoek.getFreeBytes()));

        if (logBoek.getLogLijst() != null) {
            final ListView lv1 = (ListView) findViewById(R.id.listView1);
            lv1.setAdapter(new CustomListAdapter(this, logBoek.getLogLijst()));
        }
        ToonMelding();
    }

    private void SorteerLogBoek() {
        if (logBoek.getLogLijst() != null && !logBoek.getLogLijst().isEmpty()) {
            Collections.sort(logBoek.getLogLijst(), new LogComparator());
        }
    }

    private void setFtf(ArrayList<LogItem> logLijst) {

        boolean ftfTonen = logBoek.getFtfTonen();

        if (ftfTonen) {

            int aantal = Math.min(3, logLijst.size());
            String[] ftf = {" (FTF!)", " (STF!)", " (TTF!)"};
            for (int i = 0; i < aantal; i++) {
                LogItem li = logLijst.get(logLijst.size() - i - 1);
                li.setFtf(ftf[i]);
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private LogItem LogHetLogboek() {

        String naam = preferences.getString("Gebruikersnaam", "").trim();

        // Controleer of reeds gelogd
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

        LogItem li = FindLogItemByCacherName(naam);
        if (li != null) {
            logBoek.setStatus(StatusLogBoek.ReedsGelogd);

            Toast.makeText(
                    MainActivity.this,
                    naam + " is reeds gelogd op "
                            + dateFormat.format(li.getDatum()),
                    Toast.LENGTH_LONG).show();
            return li;
        }

        // Voeg item toe aan het logboek
        li = new LogItem();
        li.setCacher(naam);
        Calendar cal = Calendar.getInstance();
        li.setDatum(cal.getTime());
        logBoek.getLogLijst().add(0, li);

        return li;
    }

    private LogItem FindLogItemByCacherName(String naam) {
        for (LogItem li : logBoek.getLogLijst()) {
            if (li.getCacher().equalsIgnoreCase(naam)) {
                return li;
            }
        }
        return null;
    }

    private void GevondenMetGeluidje() {
        try {
            MaakFileGevonden();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (mp == null) {
            mp = MediaPlayer.create(MainActivity.this, R.raw.geluidje);
        }
        if (!mp.isPlaying()) {
            mp.start();
        }
        Button button = (Button) findViewById(R.id.button1);
        button.setVisibility(View.VISIBLE);
    }

    private void CheckNfcHardwareAndStatus() {
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            logBoek.setFoutTekst("Deze telefoon heeft geen NFC chip. U kunt niet loggen met NFC");
            return;
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (!nfcAdapter.isEnabled()) {
            logBoek.setFoutTekst("De telefoon heeft een NFC chip, maar deze staat niet aan. Ga naar telefoon instellingen en zet NFC aan.");
        }
    }

    private Tag LeesNfc(Intent data) {

        // The reference to the tag that invoked us is passed as a parameter
        // (intent extra EXTRA_TAG)
        Tag tag = data.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        int totalSize = 0;
        Ndef ndefTag = Ndef.get(tag);

        // Retrieve the information from tag and display it
        StringBuilder tagInfo = new StringBuilder();

        // Get tag's NDEF messages: The NDEF messages are passed as
        // parameters (intent
        // extra EXTRA_NDEF_MESSAGES) and have to be casted into an
        // NdefMessage array.
        Parcelable[] ndefRaw = data
                .getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage[] ndefMsgs = null;
        if (ndefRaw != null) {
            ndefMsgs = new NdefMessage[ndefRaw.length];
            for (int i = 0; i < ndefMsgs.length; ++i) {
                // Cast each NDEF message from Parcelable to
                // NdefMessage:
                ndefMsgs[i] = (NdefMessage) ndefRaw[i];
                totalSize += ndefMsgs[i].toByteArray().length;
            }
        }

        if (ndefMsgs != null) {
            try {
                // Iterate through all NDEF messages on the tag:
                for (NdefMessage ndefMsg : ndefMsgs) {
                    // Get NDEF message's records:
                    NdefRecord[] records = ndefMsg.getRecords();

                    // Iterate through all NDEF records:
                    // Test if this record is a URI record:
                    for (NdefRecord record : records) {
                        if ((record.getTnf() == NdefRecord.TNF_WELL_KNOWN)
                                && Arrays.equals(record.getType(),
                                NdefRecord.RTD_TEXT)) {

                            byte[] payload = record.getPayload();
                            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8"
                                    : "UTF-16";
                            int langCodeLen = payload[0] & 63;

                            tagInfo.append(new String(payload,
                                    langCodeLen + 1, payload.length
                                    - langCodeLen - 1, textEncoding));
                        }
                    }
                }
            } catch (Exception ex) {
                logBoek.setFoutTekst("Fout tijdens lezen van de NFC Tag.");
                return tag;
            }
        }

        // Zet de tagInfo om naar een LogBoek
        logBoek = LogBoek.LogBoekFromTagInfo(tagInfo.toString());
        logBoek.setFreeBytes(ndefTag.getMaxSize() - totalSize);

        return tag;
    }

    private void SchrijfNfc(Tag tag) {

        Ndef ndefTag = Ndef.get(tag);

        try {
            String txt = logBoek.toString();
            String lang = "en";
            byte[] textBytes = txt.getBytes();
            byte[] langBytes = lang.getBytes("US-ASCII");
            int langLength = langBytes.length;
            int textLength = textBytes.length;
            byte[] payload = new byte[1 + langLength + textLength];

            // set status byte (see NDEF spec for actual bits)
            payload[0] = (byte) langLength;

            // copy langbytes and textbytes into payload
            System.arraycopy(langBytes, 0, payload, 1, langLength);
            System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

            NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT, new byte[0], payload);

            // Create NDEF message from URI record:
            NdefMessage msg = new NdefMessage(new NdefRecord[]{record});

            if (ndefTag != null) {
                if (ndefTag.getMaxSize() < msg.toByteArray().length) {
                    logBoek.setFoutTekst("Loggen niet mogelijk, er is niet genoeg vrije ruimte op de NFC Tag.");
                    return;
                }

                // Connect to tag:
                ndefTag.connect();

                // Write NDEF message:
                ndefTag.writeNdefMessage(msg);
            }
        } catch (Exception e) {
            logBoek.setFoutTekst("Fout bij het schrijven van de NFC tag.");
        } finally {
            // Close connection:
            try {
                if (ndefTag != null) {
                    ndefTag.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void resolveIntent(Intent data, boolean foregroundDispatch) {
        this.setIntent(data);

        // We were started from the recent applications history: just show our
        // main activity
        // (otherwise, the last intent that invoked us will be re-processed)
        if ((data.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
            return;
        }

        if (!foregroundDispatch) {
            return;
        }

        String action = data.getAction();

        if (!NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            return;
        }

        logBoek.setStatus(StatusLogBoek.Ok);

        Checks();
        if (logBoek.getStatus() == StatusLogBoek.Fout) {
            return;
        }

        Tag tag = LeesNfc(data);

        if (logBoek.getStatus() == StatusLogBoek.Fout) {
            return;
        }

// Controleer of we (N)FC Wageningen herkennen. Zo Nee, initialiseer de tag opnieuw
        if (!logBoek.getCacheNaam().equals(NFCWageningen)) {
            logBoek.setCacheNaam(NFCWageningen);
            logBoek.setFtfTonen(false);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            logBoek.setDatumLogBoek(cal.getTime());
            logBoek.setLogLijst(new ArrayList<LogItem>());
        }
        LogItem li = LogHetLogboek();

        if (logBoek.getStatus() == StatusLogBoek.Fout) {
            return;
        }

        if (logBoek.getStatus() == StatusLogBoek.ReedsGelogd) {
            ToonLogboek();
            GevondenMetGeluidje();
            return;
        }

        // Hier begint het wegschrijven
        SchrijfNfc(tag);
        if (logBoek.getStatus() == StatusLogBoek.Fout) {
            return;
        }

        ToonLogboek();
        GevondenMetGeluidje();

        // Toon melding van succesvolle logging
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        String naam = preferences.getString("Gebruikersnaam", "").trim();

        Toast.makeText(
                MainActivity.this,
                naam + " is succesvol gelogd op "
                        + dateFormat.format(li.getDatum()), Toast.LENGTH_LONG)
                .show();
    }

    private void Checks() {
        CheckNfcHardwareAndStatus();
        if (logBoek.getStatus() == LogBoek.StatusLogBoek.Fout) {
            return;
        }

        CheckNaam();
        if (logBoek.getStatus() == LogBoek.StatusLogBoek.Fout) {
            return;
        }
        CheckVolume();

    }

    private void CheckVolume() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int actVol = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        String vol = "(" + actVol + "/" + maxVol + ")";

        if (actVol <= 0.25 * maxVol) {
            Toast.makeText(
                    MainActivity.this,
                    "Het geluidsvolume voor media "
                            + vol
                            + " van uw telefoon staat laag. Zet het geluidsvolume voor media hoger.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void CheckNaam() {
        String naam = preferences.getString("Gebruikersnaam", "").trim();

        if (naam.isEmpty()) {
            logBoek.setFoutTekst("Om te kunnen loggen moet uw uw gebruikersnaam bekend zijn. Ga naar instellingen en voer uw gebruikersnaam in.");
            return;
        }

        if (naam.contains(";")) {
            logBoek.setFoutTekst("Een punt-komma in de naam is niet toegestaan. Ga naar instellingen en pas uw gebruikersnaam aan.");
        }
    }

    private void ToonMelding() {

        final TextView tv3 = (TextView) findViewById(R.id.textView3);
        ArrayList<LogItem> ll = logBoek.getLogLijst();
        if (ll != null && !ll.isEmpty()) {
            tv3.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (logBoek.getStatus() == LogBoek.StatusLogBoek.Fout) {
            sb.append(logBoek.getFoutTekst());
            sb.append("\r\n\r\n");
        }

        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
            sb.append("Houd de rug van de telefoon tegen de NFC sticker aan....");
        }

        tv3.setText(sb.toString());
    }

    private Boolean AlGevonden() {
        File file = new File(getFilesDir(), Gevonden);
        if (file.exists()) {
            if (file.length() > 0) {
                return true;
            }
        }
        return false;
    }

    private void MaakFileGevonden() throws FileNotFoundException {

        if (AlGevonden()) {
            return;
        }

        FileOutputStream fos = openFileOutput(Gevonden, Context.MODE_PRIVATE);
        try {
            fos.write("Ja!".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
