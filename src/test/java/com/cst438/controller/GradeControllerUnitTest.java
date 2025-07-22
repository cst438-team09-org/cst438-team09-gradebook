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
public class GradeControllerUnitTest {

    @Autowired
    private WebTestClient client ;
    @Autowired
    private GradeRepository gradeRepository;

    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    //for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    RegistrarServiceProxy registrarService;
    Random random = new Random();

    @Test
    public void testGetAndUpdateGrades() throws Exception {

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


        EntityExchangeResult<List<GradeDTO>> gradeResponse =  client.get().uri("/assignments/1/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class).returnResult();
        List<GradeDTO> gradeDTOS = gradeResponse.getResponseBody();

        ArrayList<GradeDTO> updates = new ArrayList<GradeDTO>();
        for (GradeDTO g : gradeDTOS) {
            updates.add(new GradeDTO(
                    g.gradeId(),
                    g.studentName(),
                    g.studentEmail(),
                    g.assignmentTitle(),
                    g.courseId(),
                    g.sectionId(),
                    95



            ));



        }
        EntityExchangeResult<Void> updateResponse =  client.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class).returnResult();

        for(GradeDTO update: updates){
            Grade grade = gradeRepository.findById(update.gradeId()).orElse(null);
            assertNotNull(grade);
            assertEquals(95, grade.getScore());
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


        client.get().uri("/assignments/1/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='Logged in user is not instructor for the given assignment')]").exists();

        ArrayList<GradeDTO> updates = new ArrayList<GradeDTO>();
        updates.add(new GradeDTO(
                1, null, null, null, null, 1, 50
        ));
        client.put().uri("/grades")
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
    public void testInvalidAssignmentAndGrade() throws Exception {

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


        client.get().uri("/assignments/2/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='assignment id not found')]").exists();

        ArrayList<GradeDTO> updates = new ArrayList<GradeDTO>();
        updates.add(new GradeDTO(
                2, null, null, null, null, 1, 50
        ));
        client.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updates)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                // check the list of validation messages
                .jsonPath("$.errors[?(@=='grade id not found')]").exists();
    }

}
