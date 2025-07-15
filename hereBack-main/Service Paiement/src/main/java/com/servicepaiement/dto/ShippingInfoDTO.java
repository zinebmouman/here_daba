package com.servicepaiement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingInfoDTO {
    private String fullName;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private String phone;
    private String email;
}