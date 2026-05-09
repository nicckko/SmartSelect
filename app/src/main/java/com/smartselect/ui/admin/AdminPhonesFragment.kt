package com.smartselect.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.smartselect.data.repository.PhoneRepository
import com.smartselect.databinding.FragmentAdminPhonesBinding
import com.smartselect.utils.Resource
import com.smartselect.utils.hide
import com.smartselect.utils.show
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminPhonesFragment : Fragment() {

    private var _binding: FragmentAdminPhonesBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var phoneRepository: PhoneRepository
    private lateinit var adapter: AdminPhoneAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminPhonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminPhoneAdapter(
            onEdit = { phone -> AddEditPhoneDialog.newInstance(phone).show(childFragmentManager, "edit") },
            onDelete = { phone ->
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Phone")
                    .setMessage("Are you sure you want to delete ${phone.brand} ${phone.model}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        lifecycleScope.launch {
                            phoneRepository.deletePhone(phone.id)
                            showSnackbar("${phone.model} deleted")
                        }
                    }
                    .show()
            }
        )

        binding.rvPhones.apply {
            this.adapter = this@AdminPhonesFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(false)
        }

        binding.btnAddPhone.setOnClickListener {
            AddEditPhoneDialog().show(childFragmentManager, "add")
        }



        lifecycleScope.launch {
            phoneRepository.getPhones().collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.show()
                    }
                    is Resource.Success -> {
                        binding.progressBar.hide()
                        val phones = resource.data ?: emptyList()
                        adapter.submitList(phones)
                        binding.tvCount.text = "${phones.size} phones"
                        binding.tvEmpty.visibility = if (phones.isEmpty()) View.VISIBLE else View.GONE
                    }
                    is Resource.Error -> {
                        binding.progressBar.hide()
                        showSnackbar(resource.message ?: "Error loading phones")
                    }
                }
            }
        }
    }

    private fun showSnackbar(msg: String) {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root, msg, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
