package fr.gouv.interieur.dso.repository;

import fr.gouv.interieur.dso.models.Demo;

import java.util.List;

public interface DemoRepository {
    public List<Demo> getAll();
}
