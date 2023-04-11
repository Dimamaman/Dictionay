package uz.gita.dimadictionary.presenter.screen.en_uz

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import uz.gita.dimadictionary.R
import uz.gita.dimadictionary.data.source.local.entity.DictionaryEntity
import uz.gita.dimadictionary.databinding.FragmentEnUzBinding
import uz.gita.dimadictionary.presenter.adapter.MyEnUzAdapter
import uz.gita.dimadictionary.presenter.screen.main.viewModel.MainViewModel
import uz.gita.dimadictionary.presenter.screen.main.viewModel.impl.MainViewModelImpl
import java.util.*

class EnUzFragment : Fragment(R.layout.fragment_en_uz), TextToSpeech.OnInitListener {
    private val binding by viewBinding(FragmentEnUzBinding::bind)
    private val viewModelEnglish: MainViewModel by activityViewModels<MainViewModelImpl>()

    private val myAdapter: MyEnUzAdapter by lazy { MyEnUzAdapter() }
    private var tts: TextToSpeech? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = with(binding) {

        viewModelEnglish.getEnglishWord.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.progress.show()
            } else {
                binding.progress.hide()
            }
            myAdapter.submitList(it)
        }

        myAdapter.showDialogClickListener { dictionary ->
            val dialog = Dialog(requireContext())
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.dialog_custom_en)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val englishText: AppCompatTextView = dialog.findViewById(R.id.text_english_word_dialog)
            val transcript: AppCompatTextView =
                dialog.findViewById(R.id.text_english_transcript_dialog)
            val uzbek: AppCompatTextView = dialog.findViewById(R.id.text_uzbek_dialog)

            englishText.text = dictionary.english
            transcript.text = dictionary.transcript
            uzbek.text = dictionary.uzbek

            val okBtn: AppCompatTextView = dialog.findViewById(R.id.okay_btn_dialog)
            okBtn.setOnClickListener { dialog.dismiss() }

            val soundBtn: AppCompatImageView = dialog.findViewById(R.id.im_sound_dialog)
            val copyBtn: AppCompatImageView = dialog.findViewById(R.id.im_copy_dialog)
            val favouriteBtn: AppCompatImageView = dialog.findViewById(R.id.im_favorite_dialog)
            if (dictionary.favourite == 0) {
                favouriteBtn.setImageResource(R.drawable.not_favorite)
            } else {
                favouriteBtn.setImageResource(R.drawable.favorite)
            }


            soundBtn.setOnClickListener {
                speakOut(dictionary.english)
            }

            copyBtn.setOnClickListener {
                val clipBoard =
                    requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(
                    dictionary.english, "${dictionary.english}\n\n${dictionary.transcript}\n\n" +
                            "${dictionary.uzbek}\n"
                )
                clipBoard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Copied!", Toast.LENGTH_SHORT).show()
            }

            favouriteBtn.setOnClickListener {
                if (dictionary.favourite == 0) {
                    dictionary.favourite = 1
                    viewModelEnglish.updateDictionary(dictionary)
                    favouriteBtn.setImageResource(R.drawable.favorite)
                } else {
                    favouriteBtn.setImageResource(R.drawable.not_favorite)
                    dictionary.favourite = 0
                    viewModelEnglish.updateDictionary(dictionary)
                }
            }

            okBtn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.create()
            dialog.show()
        }

        initRecyclerView()

        tts = TextToSpeech(requireContext(), this@EnUzFragment)

        lifecycleScope.launch {
            viewModelEnglish.getAllEnglishWord()
        }

        viewModelEnglish.getAllEnglishWord.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.progress.show()
            } else {
                binding.progress.hide()
            }
            myAdapter.submitList(it)
        }
    }

    private fun initRecyclerView() {
        binding.apply {
            recyclerview.adapter = myAdapter
            recyclerview.addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language not supported!")
            }
        }
    }

    private fun speakOut(englishWord: String) {
        tts!!.speak(englishWord, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private val observer = androidx.lifecycle.Observer<List<DictionaryEntity>> {
        Toast.makeText(requireContext(), it.size, Toast.LENGTH_SHORT).show()
        myAdapter.submitList(it)
    }
}