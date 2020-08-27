package com.app.repo;

import com.app.ApplicationConfiguration;
import com.app.SortingAndOrderArguments;
import com.app.domain.Book;
import com.app.domain.Genre;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import io.micronaut.transaction.annotation.ReadOnly;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Singleton
public class GenreRepositoryImpl implements GenreRepository {
    
    private final EntityManager entityManager;  
    private final ApplicationConfiguration applicationConfiguration;

    public GenreRepositoryImpl(EntityManager entityManager,
                               ApplicationConfiguration applicationConfiguration) { 
        this.entityManager = entityManager;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    @ReadOnly  
    public Optional<Genre> findById(@NotNull Long id) {
        Query q = entityManager.createQuery("SELECT o FROM Genre o JOIN FETCH o.books i WHERE o.id = :id");
        q.setParameter("id", id);
        return Optional.ofNullable((Genre) q.getSingleResult());
    }

    @Override
    @Transactional  
    public Genre save(@NotBlank String name) {
        Genre genre = new Genre(name);
        Book book = new Book("isbn", "name", genre);
        entityManager.persist(genre);
        entityManager.persist(book);
        return genre;
    }

    @Override
    @Transactional 
    public void deleteById(@NotNull Long id) {
        findById(id).ifPresent(entityManager::remove);
    }

    private final static List<String> VALID_PROPERTY_NAMES = Arrays.asList("id", "name");
    private SortingAndOrderArguments args;
    private SortingAndOrderArguments args2;

    @ReadOnly
    public List<Genre> findAll(@NotNull SortingAndOrderArguments args) {
        args2 = args;
        this.args = args;
        String qlString = "SELECT g FROM Genre as g";
        if (args.getOrder().isPresent() && args.getSort().isPresent() && VALID_PROPERTY_NAMES.contains(args.getSort().get())) {
                qlString += " ORDER BY g." + args.getSort().get() + " " + args.getOrder().get().toLowerCase();
        }
        TypedQuery<Genre> query = entityManager.createQuery(qlString, Genre.class);
        query.setMaxResults(args.getMax().orElseGet(applicationConfiguration::getMax));
        args.getOffset().ifPresent(query::setFirstResult);

        return query.getResultList();
    }

    @Override
    @Transactional 
    public int update(@NotNull Long id, @NotBlank String name) {
        return entityManager.createQuery("UPDATE Genre g SET name = :name where id = :id")
                .setParameter("name", name)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override 
    @Transactional 
    public Genre saveWithException(@NotBlank String name) {
        save(name);
        throw new PersistenceException();
    }
}