package com.medicare.service;

import com.medicare.dto.BillRequestDto;
import com.medicare.dto.BillResponseDto;
import com.medicare.dto.BillUpdateRequestDto;
import com.medicare.entity.PaymentStatus;

import java.util.List;

public interface BillService {

    // patientId is an optional filter that only staff can use.
    List<BillResponseDto> getAllBills(Long patientId);

    BillResponseDto getBillById(Long id);

    BillResponseDto createBill(BillRequestDto dto);

    BillResponseDto updateBill(Long id, BillUpdateRequestDto dto);

    BillResponseDto updateBillStatus(Long id, PaymentStatus newStatus);
}
