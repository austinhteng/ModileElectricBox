package com.example.eocproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.eocproject.databinding.PlayFragBinding

class PlayGame : Fragment() {
    companion object {
        fun newInstance() : PlayGame {
            return PlayGame()
        }

        val playFragTag = "playFragTag"
    }
    private lateinit var binding: PlayFragBinding
    private var rows = 10
    private var cols = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = PlayFragBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.gameBoard.addView(GameBoard(requireActivity(), rows, cols))
        ItemBag(binding.itemsHolder, requireContext())
        Log.d("Play Game Fragment", "view Created")
    }
}