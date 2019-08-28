package com.mer1103.prefixremover;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //Activity Context
    private Context context;

    //Permission handler var
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    //Contact list stuff
    private ContactListAdapter adapter;
    private ArrayList<Contact> contactsArrayList =  new ArrayList<>();
    private ArrayList<Contact> selectedContacts =  new ArrayList<>(); // This is not used...

    // Search by name stuff
    private EditText search_feature;
    private int textlength = 0;
    private ArrayList<Contact> sorted_arraylist = new ArrayList<>();

    //Views, layouts and stuff
    private ListView list;
    private View progressBar;
    private View list_wrapper;
    private LayoutInflater header_inflater;
    private ViewGroup header;

    private AlertDialog.Builder builder;
    private AlertDialog.Builder builder2;
    AlertDialog working_dialog;

    private int exec_count = 0;
    private int contact_i = 0;
    private int contact_j = 0;
    private int contact_step = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        //Toolbar setup
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(false);
        actionbar.setTitle(0);

        builder2 = new AlertDialog.Builder(context);
        builder2.setMessage(getString(R.string.fixing_dialog_text));
        working_dialog = builder2.create();

        //Main FAB listener
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handle_fab_click();
            }
        });

        //Initialize views, layouts, adapters etc
        progressBar = (View) findViewById(R.id.progressbar_wrapper);
        list_wrapper = (View) findViewById(R.id.list_wrapper);
        adapter = new ContactListAdapter(this,contactsArrayList);
        list = (ListView) findViewById(R.id.contacts_list_view);

        header_inflater = getLayoutInflater();
        header = (ViewGroup)header_inflater.inflate(R.layout.contact_list_header,list,false);
        list.addHeaderView(header,null,true);


        //Setting Select All check status..
        final View header_wrapper = header.findViewById(R.id.header_wrapper);
        String selected = header_wrapper.getTag().toString();
        ImageView select_all_check_checked = header.findViewById(R.id.select_all_check_checked);
        ImageView select_all_check_unchecked = header.findViewById(R.id.select_all_check_unchecked);
        if (selected.equals("true")){
            select_all_check_checked.setVisibility(View.VISIBLE);
            select_all_check_unchecked.setVisibility(View.GONE);
        }else{
            select_all_check_checked.setVisibility(View.GONE);
            select_all_check_unchecked.setVisibility(View.VISIBLE);
        }

        //Select All click listener
        header_wrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handle_select_all_click();
            }
        });

        //Contact item listener
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Object object = adapterView.getItemAtPosition(position);
                Contact contact = (Contact)object;
                contact.setChecked(!contact.isChecked());
                handle_selected_contact(contact);
                adapter.notifyDataSetChanged();
                Toast.makeText(context, get_selected_contacts_count() + " " + getString(R.string.selected_contacts), Toast.LENGTH_LONG).show();
            }
        });

        //Search by name listener
        search_feature = (EditText) findViewById(R.id.search_feature);
        search_feature.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                textlength = search_feature.getText().length();
                sorted_arraylist.clear();
                for (int i = 0; i < contactsArrayList.size(); i++) {
                    if (textlength <= contactsArrayList.get(i).getName().length()) {
                        if (clean_text(contactsArrayList.get(i).getName().toLowerCase().trim()).contains(
                                clean_text(search_feature.getText().toString().toLowerCase().trim()))) {
                            sorted_arraylist.add(contactsArrayList.get(i));
                        }
                    }
                }
                adapter = new ContactListAdapter(MainActivity.this,sorted_arraylist);
                list.setAdapter(adapter);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //Swipe Refresh listener
        final SwipeRefreshLayout mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        mySwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary);

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        mySwipeRefreshLayout.setRefreshing(true);
                        refresh_list();
                    }
                }
        );

        //Permission handler
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            find_contacts();
        }

    }

    //Remove spaces, and weird chars
    private String clean_text(String text){
        text = text.replace('á','a');
        text = text.replace('é','e');
        text = text.replace('í','i');
        text = text.replace('ó','o');
        text = text.replace('ú','u');
        text = text.replace('ñ','n');
        return text;
    }

    //Clean all lists and refresh adapter
    private void refresh_list(){
        contactsArrayList.clear();
        sorted_arraylist.clear();
        selectedContacts.clear();

        View header_wrapper = header.findViewById(R.id.header_wrapper);
        header_wrapper.setTag("false");
        ImageView select_all_check_checked = header.findViewById(R.id.select_all_check_checked);
        ImageView select_all_check_unchecked = header.findViewById(R.id.select_all_check_unchecked);
        select_all_check_checked.setVisibility(View.GONE);
        select_all_check_unchecked.setVisibility(View.VISIBLE);

        find_contacts();
        adapter.notifyDataSetChanged();
    }

    //Show confirmation dialog
    private void handle_fab_click(){

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.dismiss();
                        handle_prefix_fixer();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        builder = new AlertDialog.Builder(context);
        builder.setMessage(get_selected_contacts_count() + " " + getString(R.string.fix_dialog_text)).setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
    }

    private void handle_prefix_fixer(){
        ArrayList<Contact> selected_list = get_selected_contacts_list();
        int selected_size = selected_list.size();

        working_dialog.show();

        execute_prefix_fixer(contactsArrayList);


        /*
        if (selected_size < contact_step){
            execute_prefix_fixer(selected_list);
            Log.d("FIXING","done small");
        }else{
            contact_i = 0;
            contact_j = contact_step;
            while (contact_i < selected_size){
                ArrayList<Contact> selected_list_slice = new ArrayList<>();
                for(int i = contact_i; i < contact_j; i++){
                    if (i < selected_size){
                        selected_list_slice.add(selected_list.get(i));
                    }
                }
                execute_prefix_fixer(selected_list_slice);
                contact_i = contact_j;
                contact_j += contact_step;
            }
            Log.d("FIXING","done large");
        }*/

        //working_dialog.dismiss();
        //refresh_list();

    }

    private void execute_prefix_fixer(ArrayList<Contact> contact_list_to_fix){
        final ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        for(int i = 0; i < contact_list_to_fix.size(); i++){
            if (contact_list_to_fix.get(i).isChecked()){
                Contact c = contact_list_to_fix.get(i);
                Log.d("FIXING",c.getName());
                String fixed_number = fix_prefix(c.getPhone());
                String selectPhone = ContactsContract.Data._ID + "=? AND " + ContactsContract.Data.MIMETYPE + "='"  +
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'" + " AND " + ContactsContract.CommonDataKinds.Phone._ID + "=?";
                String[] phoneArgs = new String[]{c.getId(), c.getPhone_id()};
                ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withYieldAllowed(true)
                        .withSelection(selectPhone, phoneArgs)
                        //.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, c.getPhone())
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, fixed_number)
                        .build());
            }
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                }catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            View header_wrapper = header.findViewById(R.id.header_wrapper);
                            String was_select_all = header_wrapper.getTag().toString();
                            working_dialog.dismiss();
                            refresh_list();
                            int size = contactsArrayList.size();
                            if (size > 0 && was_select_all.equals("true")){
                                Toast.makeText(MainActivity.this, getString(R.string.missing_contacts_message), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }).start();

    }

    //Select all contacts in list
    private void handle_select_all_click(){
        View header_wrapper = header.findViewById(R.id.header_wrapper);
        ImageView select_all_check_checked = header.findViewById(R.id.select_all_check_checked);
        ImageView select_all_check_unchecked = header.findViewById(R.id.select_all_check_unchecked);
        String selected = header_wrapper.getTag().toString();
        boolean selected_value = false;
        if (selected.equals("true")){
            select_all_check_checked.setVisibility(View.GONE);
            select_all_check_unchecked.setVisibility(View.VISIBLE);
            header_wrapper.setTag("false");
            selected_value = false;
            selectedContacts.clear();
        }else{
            select_all_check_checked.setVisibility(View.VISIBLE);
            select_all_check_unchecked.setVisibility(View.GONE);
            header_wrapper.setTag("true");
            selected_value = true;
        }
        for(int i = 0; i < contactsArrayList.size(); i++){
            contactsArrayList.get(i).setChecked(selected_value);
        }
        adapter.notifyDataSetChanged();

        Toast.makeText(this, get_selected_contacts_count() + " "+ getString(R.string.selected_contacts), Toast.LENGTH_LONG).show();
    }

    //Count selected contacts
    private int get_selected_contacts_count(){
        int selected = 0;
        for(int i = 0; i < contactsArrayList.size(); i++){
            if (contactsArrayList.get(i).isChecked()){
                selected++;
            }
        }
        return selected;
    }

    //Get selected contacts list
    private ArrayList<Contact> get_selected_contacts_list(){
        ArrayList<Contact> selected_list =  new ArrayList<>();
        for(int i = 0; i < contactsArrayList.size(); i++){
            if (contactsArrayList.get(i).isChecked()){
                selected_list.add(contactsArrayList.get(i));
            }
        }
        return selected_list;
    }

    //Check or unchecks a contact
    private void handle_selected_contact(Contact contact){
        if (contact.isChecked()){
            selectedContacts.add(contact);
        }else{
            selectedContacts.remove(contact);
        }
    }

    //Normalizes phones
    private String fix_prefix(String original_phone){
        String aux = original_phone.replaceAll("\\D+","");
        if (aux.length() > 10){
            aux = aux.substring(aux.length() - 10 );
        }else{
            aux = aux;
        }
        return aux;
    }

    //show progressbar
    private void show_progressBar(){
        list_wrapper.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    //hide progressbar
    private void hide_progressBar(){
        progressBar.setVisibility(View.GONE);
        list_wrapper.setVisibility(View.VISIBLE);

    }

    //search contacts matching criteria (+521)
    private void find_contacts(){
        show_progressBar();
        Log.d("CONTACT: ", "LOOKING FOR CONTACTS");
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode("+521"));
        String name = "?";
        String number = "?";
        String contact_id = "?";
        String phone_id;

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone._ID }, null, null, null);

        try {
            while (contactLookup.moveToNext()) {
                contact_id = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data._ID));
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                number = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phone_id = contactLookup.getString((contactLookup.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)));

                Contact contact = new Contact(contact_id,name,number,phone_id);
                contactsArrayList.add(contact);
                sorted_arraylist.add(contact);
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
                list.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                hide_progressBar();
                SwipeRefreshLayout mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
                mySwipeRefreshLayout.setRefreshing(false);
            }
        }
        //return name;
    }


    //Other stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                find_contacts();
            } else {
                Toast.makeText(this, getString(R.string.permission_denied_text), Toast.LENGTH_SHORT).show();
                FloatingActionButton fab = findViewById(R.id.fab);
                fab.setVisibility(View.GONE);
            }
        }
    }
}
