package com.medicare.controller;

import com.medicare.dto.BillRequestDto;
import com.medicare.dto.BillResponseDto;
import com.medicare.dto.BillStatusRequestDto;
import com.medicare.dto.BillUpdateRequestDto;
import com.medicare.service.BillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    // Staff: all bills (optionally ?patientId=). Patient: their own.
    @GetMapping
    public ResponseEntity<List<BillResponseDto>> getAllBills(
            @RequestParam(required = false) Long patientId) {
        return ResponseEntity.ok(billService.getAllBills(patientId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDto> getBillById(@PathVariable Long id) {
        return ResponseEntity.ok(billService.getBillById(id));
    }

    @PostMapping
    public ResponseEntity<BillResponseDto> createBill(@Valid @RequestBody BillRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(dto));
    }

    // Edit charges/items of a PENDING bill.
    @PutMapping("/{id}")
    public ResponseEntity<BillResponseDto> updateBill(
            @PathVariable Long id, @Valid @RequestBody BillUpdateRequestDto dto) {
        return ResponseEntity.ok(billService.updateBill(id, dto));
    }

    // Settle a PENDING bill: {"status": "PAID"} or {"status": "CANCELLED"}.
    // PATCH (not PUT) because only one field changes - same convention
    // as /api/appointments/{id}/cancel.
    @PatchMapping("/{id}/status")
    public ResponseEntity<BillResponseDto> updateBillStatus(
            @PathVariable Long id, @Valid @RequestBody BillStatusRequestDto dto) {
        return ResponseEntity.ok(billService.updateBillStatus(id, dto.getStatus()));
    }
}
