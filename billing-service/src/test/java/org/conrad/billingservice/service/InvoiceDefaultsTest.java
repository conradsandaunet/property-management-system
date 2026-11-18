package org.conrad.billingservice.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceDefaultsTest {

    @Test
    void kidIsTheZeroPaddedInvoiceIdPlusAMod10CheckDigit() {
        // 0000001: weights 2,1,2... from the right give 1*2 = 2, check digit (10 - 2) % 10 = 8.
        assertThat(InvoiceDefaults.kid(1)).isEqualTo("00000018");
        // 1234567 is a common Luhn example with check digit 4.
        assertThat(InvoiceDefaults.kid(1234567)).isEqualTo("12345674");
    }

    @Test
    void everyKidPassesTheLuhnCheck() {
        for (long id = 1; id <= 500; id++) {
            assertThat(luhnValid(InvoiceDefaults.kid(id))).as("KID for %d", id).isTrue();
        }
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';
            if (doubleIt) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
