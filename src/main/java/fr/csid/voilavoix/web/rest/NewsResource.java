package fr.csid.voilavoix.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.csid.voilavoix.domain.News;

import fr.csid.voilavoix.repository.NewsRepository;
import fr.csid.voilavoix.repository.search.NewsSearchRepository;
import fr.csid.voilavoix.web.rest.util.HeaderUtil;
import fr.csid.voilavoix.web.rest.util.PaginationUtil;
import fr.csid.voilavoix.service.dto.NewsDTO;
import fr.csid.voilavoix.service.mapper.NewsMapper;
import io.swagger.annotations.ApiParam;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.elasticsearch.index.query.QueryBuilders.*;

/**
 * REST controller for managing News.
 */
@RestController
@RequestMapping("/api")
public class NewsResource {

    private final Logger log = LoggerFactory.getLogger(NewsResource.class);

    private static final String ENTITY_NAME = "news";
        
    private final NewsRepository newsRepository;

    private final NewsMapper newsMapper;

    private final NewsSearchRepository newsSearchRepository;

    public NewsResource(NewsRepository newsRepository, NewsMapper newsMapper, NewsSearchRepository newsSearchRepository) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.newsSearchRepository = newsSearchRepository;
    }

    /**
     * POST  /news : Create a new news.
     *
     * @param newsDTO the newsDTO to create
     * @return the ResponseEntity with status 201 (Created) and with body the new newsDTO, or with status 400 (Bad Request) if the news has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/news")
    @Timed
    public ResponseEntity<NewsDTO> createNews(@RequestBody NewsDTO newsDTO) throws URISyntaxException {
        log.debug("REST request to save News : {}", newsDTO);
        if (newsDTO.getId() != null) {
            return ResponseEntity.badRequest().headers(HeaderUtil.createFailureAlert(ENTITY_NAME, "idexists", "A new news cannot already have an ID")).body(null);
        }
        News news = newsMapper.newsDTOToNews(newsDTO);
        news = newsRepository.save(news);
        NewsDTO result = newsMapper.newsToNewsDTO(news);
        newsSearchRepository.save(news);
        return ResponseEntity.created(new URI("/api/news/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /news : Updates an existing news.
     *
     * @param newsDTO the newsDTO to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated newsDTO,
     * or with status 400 (Bad Request) if the newsDTO is not valid,
     * or with status 500 (Internal Server Error) if the newsDTO couldnt be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/news")
    @Timed
    public ResponseEntity<NewsDTO> updateNews(@RequestBody NewsDTO newsDTO) throws URISyntaxException {
        log.debug("REST request to update News : {}", newsDTO);
        if (newsDTO.getId() == null) {
            return createNews(newsDTO);
        }
        News news = newsMapper.newsDTOToNews(newsDTO);
        news = newsRepository.save(news);
        NewsDTO result = newsMapper.newsToNewsDTO(news);
        newsSearchRepository.save(news);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, newsDTO.getId().toString()))
            .body(result);
    }

    /**
     * GET  /news : get all the news.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of news in body
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/news")
    @Timed
    public ResponseEntity<List<NewsDTO>> getAllNews(@ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to get a page of News");
        Page<News> page = newsRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/news");
        return new ResponseEntity<>(newsMapper.newsToNewsDTOs(page.getContent()), headers, HttpStatus.OK);
    }

    /**
     * GET  /news/:id : get the "id" news.
     *
     * @param id the id of the newsDTO to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the newsDTO, or with status 404 (Not Found)
     */
    @GetMapping("/news/{id}")
    @Timed
    public ResponseEntity<NewsDTO> getNews(@PathVariable Long id) {
        log.debug("REST request to get News : {}", id);
        News news = newsRepository.findOne(id);
        NewsDTO newsDTO = newsMapper.newsToNewsDTO(news);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(newsDTO));
    }

    /**
     * DELETE  /news/:id : delete the "id" news.
     *
     * @param id the id of the newsDTO to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/news/{id}")
    @Timed
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        log.debug("REST request to delete News : {}", id);
        newsRepository.delete(id);
        newsSearchRepository.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }

    /**
     * SEARCH  /_search/news?query=:query : search for the news corresponding
     * to the query.
     *
     * @param query the query of the news search 
     * @param pageable the pagination information
     * @return the result of the search
     * @throws URISyntaxException if there is an error to generate the pagination HTTP headers
     */
    @GetMapping("/_search/news")
    @Timed
    public ResponseEntity<List<NewsDTO>> searchNews(@RequestParam String query, @ApiParam Pageable pageable)
        throws URISyntaxException {
        log.debug("REST request to search for a page of News for query {}", query);
        Page<News> page = newsSearchRepository.search(queryStringQuery(query), pageable);
        HttpHeaders headers = PaginationUtil.generateSearchPaginationHttpHeaders(query, page, "/api/_search/news");
        return new ResponseEntity<>(newsMapper.newsToNewsDTOs(page.getContent()), headers, HttpStatus.OK);
    }


}
