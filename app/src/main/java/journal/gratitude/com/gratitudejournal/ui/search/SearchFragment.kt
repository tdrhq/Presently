package journal.gratitude.com.gratitudejournal.ui.search

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.ChangeBounds
import androidx.transition.TransitionInflater
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.android.support.DaggerFragment
import journal.gratitude.com.gratitudejournal.R
import journal.gratitude.com.gratitudejournal.databinding.SearchFragmentBinding
import journal.gratitude.com.gratitudejournal.model.CLICKED_SEARCH_ITEM
import journal.gratitude.com.gratitudejournal.util.setStatusBarColorsForBackground
import journal.gratitude.com.gratitudejournal.util.textChanges
import kotlinx.android.synthetic.main.search_fragment.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject


class SearchFragment : DaggerFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel by viewModels<SearchViewModel> { viewModelFactory }
    private lateinit var binding: SearchFragmentBinding
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = SearchFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        val transition =
            TransitionInflater.from(this.activity).inflateTransition(android.R.transition.move)

        sharedElementEnterTransition = ChangeBounds().apply {
            sharedElementEnterTransition = transition
            duration = 300
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(requireContext())

        val adapter = SearchAdapter(requireActivity(), object : SearchAdapter.OnClickListener {
            override fun onClick(
                clickedDate: LocalDate
            ) {
                firebaseAnalytics.logEvent(CLICKED_SEARCH_ITEM, null)

                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.searchFragment) {
                    val directions =
                        SearchFragmentDirections.actionSearchFragmentToEntryFragment(clickedDate.toString())
                    navController.navigate(directions)
                }
            }
        })

        lifecycleScope.launch {
            adapter.loadStateFlow.collect {
                val refresher = it.refresh
                val displayEmptyMessage =
                    (refresher is LoadState.NotLoading && adapter.itemCount == 0)

                search_results?.isVisible = !displayEmptyMessage
                no_results_icon?.isVisible = displayEmptyMessage
                no_results?.isVisible = displayEmptyMessage
            }
        }

        search_results.layoutManager = LinearLayoutManager(context)
        search_results.adapter = adapter

        lifecycleScope.launch {
            search_text.textChanges()
                .debounce(300)
                .filter { charSequence -> !charSequence.isNullOrBlank() }
                .map { charSequence -> charSequence.toString() }
                .flatMapLatest { query ->
                    viewModel.search(query)
                }
                .collectLatest {results ->
                    adapter.submitData(results)
                }
        }

        search_text.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> { dismissKeyboard(); true }
                else -> false
            }
        }

        search_icon.setOnClickListener {
            dismissKeyboard()
        }

        back_icon.setOnClickListener {
            dismissKeyboard()
            findNavController().navigateUp()
        }

        openKeyboard()

        ViewCompat.setOnApplyWindowInsetsListener(container) { v, insets ->
            v.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }

        val window = requireActivity().window
        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(R.attr.toolbarColor, typedValue, true)
        setStatusBarColorsForBackground(window, typedValue.data)
        window.statusBarColor = typedValue.data
    }

    private fun openKeyboard() {
        search_text.requestFocus()
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.showSoftInput(search_text, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun dismissKeyboard() {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(search_text.windowToken, 0)
    }

}
