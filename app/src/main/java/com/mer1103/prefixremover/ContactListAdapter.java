package com.mer1103.prefixremover;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ContactListAdapter extends ArrayAdapter<String> {


    private String TAG = "ContactListAdapter";
    private final Activity context;
    private List<Contact> items = new ArrayList<Contact>();

    public ContactListAdapter(Activity context, List items) {
        super(context, R.layout.contact_list_item, items);
        this.context=context;
        this.items = items;
    }

    private String fix_prefix(String original_phone){
        String aux = original_phone.replaceAll("\\D+","");
        if (aux.length() > 10){
            aux = aux.substring(aux.length() - 10 );
        }else{
            aux = aux;
        }
        return aux;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.contact_list_item, null,true);

        Contact contact = items.get(position);

        TextView name = (TextView) rowView.findViewById(R.id.contact_name);
        TextView phone_detail = (TextView) rowView.findViewById(R.id.contact_phone_detail);
        TextView phone_detail_result = (TextView) rowView.findViewById(R.id.contact_phone_detail_result);
        ImageView contact_check_checked = (ImageView) rowView.findViewById(R.id.contact_check_checked);
        ImageView contact_check_unchecked = (ImageView) rowView.findViewById(R.id.contact_check_unchecked);

        if (contact.isChecked()){
            contact_check_checked.setVisibility(View.VISIBLE);
            contact_check_unchecked.setVisibility(View.GONE);
        }else{
            contact_check_checked.setVisibility(View.GONE);
            contact_check_unchecked.setVisibility(View.VISIBLE);
        }

        name.setText(contact.getName());


        String phone_detail_str = this.context.getString(R.string.number_to_fix) + " " + contact.getPhone();
        String phone_detail_result_str = this.context.getString(R.string.fixed_result) + " " + fix_prefix(contact.getPhone());

        phone_detail.setText(phone_detail_str);
        phone_detail_result.setText(phone_detail_result_str);

        return rowView;
    }

}
