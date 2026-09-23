package fr.gouv.interieur.dso.repository.impl;

import fr.gouv.interieur.dso.models.Demo;
import fr.gouv.interieur.dso.repository.DemoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class DemoRepositoryImpl  implements DemoRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Demo> getAll() {
        String query = "select id, name from demo;";
        List<Demo> demoList = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);
        rows.forEach(map -> {
            Demo demo = new Demo();
            demo.setId(Integer.parseInt(String.valueOf(map.get(("id")))));
            demo.setName(String.valueOf(map.get("name")));
            demoList.add(demo);
        });
        return demoList;
    }
}
