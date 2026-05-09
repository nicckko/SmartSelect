package com.smartselect.ui.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.smartselect.R
import com.smartselect.databinding.DialogOrderSuccessBinding

class OrderSuccessDialog : DialogFragment() {

    private var _binding: DialogOrderSuccessBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_CODE = "pickup_code"
        private const val ARG_DATE = "pickup_date"

        fun newInstance(pickupCode: String, pickupDate: String): OrderSuccessDialog {
            return OrderSuccessDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CODE, pickupCode)
                    putString(ARG_DATE, pickupDate)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogOrderSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)

        val code = arguments?.getString(ARG_CODE) ?: "------"
        val date = arguments?.getString(ARG_DATE) ?: "—"

        binding.tvPickupCode.text = code
        binding.tvPickupDate.text = date

        binding.btnDone.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_checkout_to_home)
            } catch (e: Exception) {
                try {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_checkout_to_home)
                } catch (e2: Exception) {
                    dismiss()
                }
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}