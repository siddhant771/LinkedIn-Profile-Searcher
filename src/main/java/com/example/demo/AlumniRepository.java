package com.example.demo;



import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AlumniRepository {

    private final JdbcTemplate jdbc;

    public AlumniRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void saveAll(List<AlumniProfile> profiles) {
        String sql = """
                INSERT INTO alumni
                (linkedin_url, name, role_title, university,
                 location, linkedin_headline, passout_year)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (linkedin_url) DO UPDATE SET
                    name = EXCLUDED.name,
                    role_title = EXCLUDED.role_title,
                    university = EXCLUDED.university,
                    location = EXCLUDED.location,
                    linkedin_headline = EXCLUDED.linkedin_headline,
                    passout_year = EXCLUDED.passout_year
                """;

        for (AlumniProfile p : profiles) {
            jdbc.update(sql,
                    p.linkedinUrl(), p.name(), p.currentRole(),
                    p.university(), p.location(),
                    p.linkedinHeadline(), p.passoutYear());
        }
    }

    public List<AlumniProfile> findAll(int page, int size) {
        return jdbc.query("""
                SELECT * FROM alumni
                ORDER BY id
                LIMIT ? OFFSET ?
                """,
                (row, number) -> new AlumniProfile(
                        row.getString("name"),
                        row.getString("role_title"),
                        row.getString("university"),
                        row.getString("location"),
                        row.getString("linkedin_headline"),
                        row.getObject("passout_year", Integer.class),
                        row.getString("linkedin_url")
                ),
                size, (long) page * size);
    }
}
