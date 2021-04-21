package com.proquest.interview.phonebook;

// PhoneBookFactory
public class PhoneBookFactory {
    public static PhoneBook createPhoneBook(){
        return new PhoneBookImpl();
    }
}
