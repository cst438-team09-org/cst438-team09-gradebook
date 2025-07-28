package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // get Sections for an instructor
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {
        // return the Sections that have instructorEmail for the 
        // logged in instructor user for the given term.
        String email = principal.getName();
        List<Section> sections = sectionRepository.findByInstructorEmailAndYearAndSemester(email, year, semester);
        return sections.stream()
                .map(s -> {
                    User instr = userRepository.findByEmail(s.getInstructorEmail());
                    String name = instr != null ? instr.getName() : "";
                    return new SectionDTO(
                            s.getSectionNo(),
                            s.getTerm().getYear(),
                            s.getTerm().getSemester(),
                            s.getCourse().getCourseId(),
                            s.getCourse().getTitle(),
                            s.getSectionId(),
                            s.getBuilding(),
                            s.getRoom(),
                            s.getTimes(),
                            name,
                            s.getInstructorEmail()
                    );
                })
                .toList();
    }

    // instructor lists assignments for a section.
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

        // verify that user is the instructor for the section
        //  return list of assignments for the Section
        Section section = sectionRepository.findById(secNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized for this section");
        }
        return section.getAssignments().stream()
                .map(a -> new AssignmentDTO(
                        a.getAssignmentId(),
                        a.getTitle(),
                        a.getDueDate().toString(),
                        section.getCourse().getCourseId(),
                        section.getSectionId(),
                        section.getSectionNo()
                ))
                .toList();
    }


    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        //  user must be the instructor for the Section
        //  check that assignment dueDate is between start date and
        //  end date of the term
        //  create and save an Assignment entity
        //  return AssignmentDTO with database generated primary key
        Section section = sectionRepository.findById(dto.secNo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section not found"));
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        if (dto.secId() != section.getSectionId() || !dto.courseId().equals(section.getCourse().getCourseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Section/course mismatch");
        }
        Date due;
        try {
            due = Date.valueOf(dto.dueDate());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dueDate format");
        }
        if (due.before(section.getTerm().getStartDate()) || due.after(section.getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be within term dates");
        }
        Assignment a = new Assignment();
        a.setTitle(dto.title());
        a.setDueDate(due);
        a.setSection(section);
        Assignment saved = assignmentRepository.save(a);
        return new AssignmentDTO(
                saved.getAssignmentId(),
                saved.getTitle(),
                saved.getDueDate().toString(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                section.getSectionNo()
        );
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        //  update Assignment Entity.  only title and dueDate fields can be changed.
        //  user must be instructor of the Section
        Assignment a = assignmentRepository.findById(dto.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment not found"));
        Section section = a.getSection();
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        Date due;
        try {
            due = Date.valueOf(dto.dueDate());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dueDate format");
        }
        if (due.before(section.getTerm().getStartDate()) || due.after(section.getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Due date must be within term dates");
        }
        a.setTitle(dto.title());
        a.setDueDate(due);
        Assignment updated = assignmentRepository.save(a);
        return new AssignmentDTO(
                updated.getAssignmentId(),
                updated.getTitle(),
                updated.getDueDate().toString(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                section.getSectionNo()
        );
    }


    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        // delete the Assignment entity
        Assignment a = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        if (!a.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }
        assignmentRepository.delete(a);
    }

    // student lists their assignments/grades  ordered by due date
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        //  return AssignmentStudentDTOs with scores of a 
        //  Grade entity exists.
        //  hint: use the GradeRepository findByStudentEmailAndAssignmentId
        //  If assignment has not been graded, return a null score.
        String email = principal.getName();
        List<Assignment> assignments = assignmentRepository.findByStudentEmailAndYearAndSemester(email, year, semester);
        return assignments.stream()
                .map(a -> {
                    Grade g = gradeRepository.findByStudentEmailAndAssignmentId(email, a.getAssignmentId());
                    Integer score = g != null ? g.getScore() : null;
                    return new AssignmentStudentDTO(
                            a.getAssignmentId(),
                            a.getTitle(),
                            a.getDueDate(),
                            a.getSection().getCourse().getCourseId(),
                            a.getSection().getSectionId(),
                            score
                    );
                })
                .toList();
    }
}

