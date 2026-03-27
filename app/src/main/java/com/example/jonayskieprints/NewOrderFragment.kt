package com.example.jonayskieprints

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.jonayskieprints.databinding.FragmentNewOrderUserBinding
import java.util.Locale

class NewOrderFragment : Fragment() {
    private var _binding: FragmentNewOrderUserBinding? = null
    private val binding get() = _binding!!
    private var gcashReceiptUri: Uri? = null

    private val total = 300.00
    private val downpayment = 150.00
    private var paymentMethod = "cash"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewOrderUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSpinners()
        setupSteppers()

        // Payment buttons
        binding.btnPayCash.setOnClickListener { setPayment("cash") }
        binding.btnPayGcash.setOnClickListener { setPayment("gcash") }

        // GCash amount toggling
        binding.rgGcashAmount.setOnCheckedChangeListener { _, checkedId ->
            updateGcashAmount()
        }
        updateGcashAmount()

        // Receipt picker
        binding.btnPickReceipt.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResult(intent, 102)
        }

        // Place order
        binding.btnPlaceOrder.setOnClickListener {
            if (paymentMethod == "gcash" && gcashReceiptUri == null) {
                Toast.makeText(requireContext(), "Please upload your GCash receipt!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "Order placed!", Toast.LENGTH_SHORT).show()
        }

        // Summary sample (you can set dynamically)
        binding.tvSummaryService.text = "Service: Print"
        binding.tvSummaryDelivery.text = "Delivery: Pickup"
        binding.tvSummaryPayment.text = "Payment: Cash on Pickup"
        binding.tvTotalAmount.text = "₱ " + String.format(Locale.getDefault(), "%.2f", total)

        setPayment("cash")

        // Delivery Option Toggling
        binding.rgDelivery.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_pickup) {
                binding.llPickupTime.visibility = View.VISIBLE
                binding.tilAddress.visibility = View.GONE
            } else {
                binding.llPickupTime.visibility = View.GONE
                binding.tilAddress.visibility = View.VISIBLE
            }
        }
    }

    private fun setupSpinners() {
        // Service Spinner
        val serviceOptions = listOf("Document Printing", "Photocopying", "Photo Development", "Laminating")
        val serviceAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, serviceOptions)
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerService.adapter = serviceAdapter

        // Pickup Time Spinner
        val pickupTimes = listOf("8:00 AM", "9:00 AM", "10:00 AM", "11:00 AM", "12:00 NN", "1:00 PM", "2:00 PM", "3:00 PM", "4:00 PM", "5:00 PM")
        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pickupTimes)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPickupTime.adapter = timeAdapter

        // Paper Size
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.paper_sizes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPaperSize.adapter = adapter
        }

        // Paper Type
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.paper_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPaperType.adapter = adapter
        }

        // Photo Size
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.photo_sizes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerPhotoSize.adapter = adapter
        }

        // Folder Size
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.folder_sizes,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFolderSize.adapter = adapter
        }

        // Folder Color
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.folder_colors,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFolderColor.adapter = adapter
        }
    }

    private fun setupSteppers() {
        binding.btnStep0Next.setOnClickListener {
            binding.llStep0.visibility = View.GONE
            binding.llStep1.visibility = View.VISIBLE
            updateStepIndicator(1)
        }

        binding.btnStep1Back.setOnClickListener {
            binding.llStep1.visibility = View.GONE
            binding.llStep0.visibility = View.VISIBLE
            updateStepIndicator(0)
        }

        binding.btnStep1Next.setOnClickListener {
            binding.llStep1.visibility = View.GONE
            binding.llStep2.visibility = View.VISIBLE
            updateStepIndicator(2)
        }

        binding.btnStep2Back.setOnClickListener {
            binding.llStep2.visibility = View.GONE
            binding.llStep1.visibility = View.VISIBLE
            updateStepIndicator(1)
        }
    }

    private fun updateStepIndicator(step: Int) {
        // Simple step indicator logic (can be improved with better drawables)
        when (step) {
            0 -> {
                binding.tvStep1.setBackgroundResource(R.drawable.bg_step_active)
                binding.tvStep2.setBackgroundResource(R.drawable.bg_step_inactive)
                binding.tvStep3.setBackgroundResource(R.drawable.bg_step_inactive)
            }
            1 -> {
                binding.tvStep1.setBackgroundResource(R.drawable.bg_step_active)
                binding.tvStep2.setBackgroundResource(R.drawable.bg_step_active)
                binding.tvStep3.setBackgroundResource(R.drawable.bg_step_inactive)
            }
            2 -> {
                binding.tvStep1.setBackgroundResource(R.drawable.bg_step_active)
                binding.tvStep2.setBackgroundResource(R.drawable.bg_step_active)
                binding.tvStep3.setBackgroundResource(R.drawable.bg_step_active)
            }
        }
    }

    private fun setPayment(method: String) {
        paymentMethod = method
        if (method == "gcash") {
            binding.llGcashPanel.visibility = View.VISIBLE
            binding.btnPayGcash.alpha = 1f
            binding.btnPayCash.alpha = 0.5f
            binding.tvSummaryPayment.text = "Payment: GCash"
        } else {
            binding.llGcashPanel.visibility = View.GONE
            binding.btnPayGcash.alpha = 0.5f
            binding.btnPayCash.alpha = 1f
            binding.tvSummaryPayment.text = "Payment: Cash on Pickup"
        }
    }

    private fun updateGcashAmount() {
        if (binding.rbFull.isChecked) {
            binding.tvGcashAmount.text = "Send exactly: ₱" + String.format(Locale.getDefault(), "%.2f", total)
        } else {
            binding.tvGcashAmount.text = "Send exactly: ₱" + String.format(Locale.getDefault(), "%.2f", downpayment)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == 102 && data != null) {
            gcashReceiptUri = data.data
            gcashReceiptUri?.let {
                binding.ivReceiptPreview.setImageURI(it)
                binding.ivReceiptPreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
