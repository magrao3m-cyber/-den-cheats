package com.example.edencheats;

/** Estado simples compartilhado entre telas (evita Parcelable). */
final class Session {
    private Session() {}

    static Model.Game game;
    static Model.Item item;
    static String query = "";
}
