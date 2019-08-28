package com.mer1103.prefixremover;

import java.util.Objects;

public class Contact {

    private String id;
    private String name;
    private String phone_id;
    private String phone;
    private boolean checked;

    public Contact(){
        this.id = "";
        this.name = "";
        this.phone = "";
        this.phone_id = "";
        this.checked = false;
    }

    public Contact(String id, String name, String phone, String phone_id){
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.phone_id = phone_id;
        this.checked = false;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone_id() {
        return phone_id;
    }

    public void setPhone_id(String phone_id) {
        this.phone_id = phone_id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }


    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Contact)) {
            return false;
        }
        Contact contact = (Contact) o;
        return contact.id.equals(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,phone_id);
    }



}
