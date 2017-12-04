package beinfiny.com.beinfinyconfig.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;

import beinfiny.com.beinfinyconfig.R;
import beinfiny.com.beinfinyconfig.dto.Assocation;
import beinfiny.com.beinfinyconfig.tools.Http;

public class MainActivity extends AppCompatActivity {

    private static final String URL = "https://beinfiny.fr/app/";
    private static final String GET_PHP = "users.php";
    private static final String SEND_PHP = "card.php";

    private Spinner spinnerUsers;
    private TextView contentId;
    private String idCentre;

    private PendingIntent pendingIntent;
    private NfcAdapter mNfcAdapter;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private ArrayList<String> usersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.spinnerUsers = (Spinner) findViewById(R.id.spinnerUser);
        this.contentId = (TextView) findViewById(R.id.contentCarte);

        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        this.pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }

        this.intentFiltersArray = new IntentFilter[]{ndef,};
        this.techListsArray = new String[][]{new String[]{
                MifareUltralight.class.getName()
        }};
        // Récupération de l'id du centre
        Intent myIntent = getIntent();
        this.idCentre = myIntent.getStringExtra(getString(R.string.idCentre));

        // Récupération des utilisateurs existants
        this.GetUsers();

        // Affichage info
        Toast.makeText(MainActivity.this, getString(R.string.toastUserEnCours), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        this.contentId.setText(this.ConvertHExToString((tag.getId())));
    }

    private String ConvertHExToString(byte[] raw) {
        return String.format("%0" + (raw.length * 2) + "X", new BigInteger(1, raw));
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mNfcAdapter.enableForegroundDispatch(this, this.pendingIntent, this.intentFiltersArray, this.techListsArray);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mNfcAdapter.disableForegroundDispatch(this);
    }

    public void Associate(View view) {
        // Envoit de l'association
        this.SendAssocation();
    }

    // Execute la récupération des utilisateurs
    private void GetUsers() {
        UserFrom userFrom = new UserFrom();
        userFrom.execute((Void) null);
    }

    // Envoie les informations d'association
    private void SendAssocation() {
        String user = (String) this.spinnerUsers.getSelectedItem();
        String idCarte = this.contentId.getText().toString();

        Assocation asso = new Assocation();
        asso.setIdCarte(idCarte);
        asso.setUser(user);

        UserTo userTo = new UserTo();
        userTo.execute(asso);
    }

    public class UserFrom extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            try {
                // Send request
                response = Http.SendGetRequest(URL + GET_PHP + "?centre=" + idCentre);
            } catch (IOException e) {
                // Test
                //response = "alexandre;rudy;jean;camille;axel;bob;marcel;toto;Marc";
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // Vérification de la réponse

            usersList = new ArrayList<>();

            // Build array for adapter
            ArrayList<String> users = new ArrayList<>();
            for (String user : result.split(";")) {
                users.add(user);
            }

            // Build and set adapter to spinner
            ArrayAdapter adapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_list_item_1, users);
            spinnerUsers.setAdapter(adapter);

            // Show first element
            spinnerUsers.setSelection(0);
        }
    }

    public class UserTo extends AsyncTask<Assocation, Void, String> {
        @Override
        protected String doInBackground(Assocation... params) {
            String response = null;
            String userId  = params[0].getUser().split(",")[0];

            try {
                // Send request
                String param = "UID_card=\"" + params[0].getIdCarte() + "\"&id_abonne=" + userId;
                response = Http.SendGetRequest(URL + SEND_PHP + "?" + param);
            } catch (IOException e) {
                // Test
                response = "OK";
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // Vérification de la réponse
            if (result.equals("OK")) {
                Toast.makeText(MainActivity.this, getString(R.string.toastSendOk), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.toastSendKo), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
