package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EnrollmentControllerUnitTest {

   @Autowired
    private WebTestClient client ;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    //for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    RegistrarServiceProxy registrarService;
    Random random = new Random();

    @Test
    public void testGetAndUpdateEnrollments() throws Exception {

        // login as admin and get the security token
        String instructorEmail = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto =  client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);


        EntityExchangeResult<List<EnrollmentDTO>> enrollmentResponse =  client.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class).returnResult();
        List<EnrollmentDTO> enrollmentDTOS = enrollmentResponse.getResponseBody();

        ArrayList<EnrollmentDTO> updates = new ArrayList<EnrollmentDTO>();
        for (EnrollmentDTO e : enrollmentDTOS) {
            updates.add(new EnrollmentDTO(
                    e.enrollmentId(),
                    "A",
                    e.studentId(),
                    e.name(),
                    e.email(),
                    e.courseId(),
                    e.title(),
                    e.sectionId(),
                    e.sectionNo(),
                    e.building(),
                    e.room(),
                    e.times(),
                    e.credits(),
                    e.year(),
                    e.semester()

            ));



        }
        EntityExchangeResult<Void> updateResponse =  client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class).returnResult();

        for(EnrollmentDTO update: updates){
            Enrollment enrollment = enrollmentRepository.findById(update.enrollmentId()).orElse(null);
            assertNotNull(enrollment);
            assertEquals("A", enrollment.getGrade());
        }

    }

    @Test
    public void testInvalidInstructor() throws Exception {

        // login as admin and get the security token
        String instructorEmail = "teddy@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);


        client.get().uri("/sections/1/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='Logged in user is not instructor for the given assignment')]").exists();

        ArrayList<EnrollmentDTO> updates = new ArrayList<EnrollmentDTO>();
        updates.add(new EnrollmentDTO(
                1, "C", 1, null, null, null, null, 1, 1, null, null, null, 4, 2025, null
        ));
        client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='Logged in user is not instructor for the given section')]").exists();
    }

    @Test
    public void testInvalidSectionAndEnrollment() throws Exception {

        // login as admin and get the security token
        String instructorEmail = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto = client.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(instructorEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);


        client.get().uri("/sections/2/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='section number not found')]").exists();

        ArrayList<EnrollmentDTO> updates = new ArrayList<EnrollmentDTO>();
        updates.add(new EnrollmentDTO(
                2, "C", 1, null, null, null, null, 1, 1, null, null, null, 4, 2025, null
        ));
        client.put().uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='enrollment id not found')]").exists();
    }
}