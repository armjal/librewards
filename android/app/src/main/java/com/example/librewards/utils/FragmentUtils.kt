package com.example.librewards.utils

import androidx.fragment.app.Fragment


abstract class FragmentExtended : Fragment() {
    abstract val title: String?
    abstract val icon: Int
}