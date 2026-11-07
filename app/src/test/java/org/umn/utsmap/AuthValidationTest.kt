package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthValidation

class AuthValidationTest {
    @Test fun requiredFieldsRejectWhitespace() {
        assertNotNull(AuthValidation.name("  "))
        assertNotNull(AuthValidation.email("  "))
        assertNotNull(AuthValidation.password("        "))
    }
    @Test fun emailsMustHaveValidDomainAndNoSpaces() {
        listOf("abc", "a@", "a@b", "a b@test.com", "a@-test.com", "a@test..com").forEach {
            assertNotNull(it, AuthValidation.email(it))
        }
        assertNull(AuthValidation.email(" certified@email.com "))
        assertNull(AuthValidation.email("user+demo@student.umn.ac.id"))
    }
    @Test fun passwordsCheckLengthAndConfirmationExactly() {
        assertNotNull(AuthValidation.password("1234567"))
        assertNull(AuthValidation.password("c3rt1f1ed"))
        assertNotNull(AuthValidation.confirmation("c3rt1f1ed", "C3rt1f1ed"))
        assertNull(AuthValidation.confirmation("c3rt1f1ed", "c3rt1f1ed"))
    }
}
