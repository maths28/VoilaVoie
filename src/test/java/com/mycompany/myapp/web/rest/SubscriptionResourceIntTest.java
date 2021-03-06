package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.VoilaVoix2App;

import com.mycompany.myapp.domain.Subscription;
import com.mycompany.myapp.repository.SubscriptionRepository;
import com.mycompany.myapp.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the SubscriptionResource REST controller.
 *
 * @see SubscriptionResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = VoilaVoix2App.class)
public class SubscriptionResourceIntTest {

    private static final String DEFAULT_LABEL = "AAAAAAAAAA";
    private static final String UPDATED_LABEL = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final String DEFAULT_API = "AAAAAAAAAA";
    private static final String UPDATED_API = "BBBBBBBBBB";

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restSubscriptionMockMvc;

    private Subscription subscription;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        SubscriptionResource subscriptionResource = new SubscriptionResource(subscriptionRepository);
        this.restSubscriptionMockMvc = MockMvcBuilders.standaloneSetup(subscriptionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Subscription createEntity(EntityManager em) {
        Subscription subscription = new Subscription()
            .label(DEFAULT_LABEL)
            .description(DEFAULT_DESCRIPTION)
            .api(DEFAULT_API);
        return subscription;
    }

    @Before
    public void initTest() {
        subscription = createEntity(em);
    }

    @Test
    @Transactional
    public void createSubscription() throws Exception {
        int databaseSizeBeforeCreate = subscriptionRepository.findAll().size();

        // Create the Subscription
        restSubscriptionMockMvc.perform(post("/api/subscriptions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscription)))
            .andExpect(status().isCreated());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeCreate + 1);
        Subscription testSubscription = subscriptionList.get(subscriptionList.size() - 1);
        assertThat(testSubscription.getLabel()).isEqualTo(DEFAULT_LABEL);
        assertThat(testSubscription.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testSubscription.getApi()).isEqualTo(DEFAULT_API);
    }

    @Test
    @Transactional
    public void createSubscriptionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = subscriptionRepository.findAll().size();

        // Create the Subscription with an existing ID
        subscription.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restSubscriptionMockMvc.perform(post("/api/subscriptions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscription)))
            .andExpect(status().isBadRequest());

        // Validate the Alice in the database
        List<Subscription> subscriptionList = subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllSubscriptions() throws Exception {
        // Initialize the database
        subscriptionRepository.saveAndFlush(subscription);

        // Get all the subscriptionList
        restSubscriptionMockMvc.perform(get("/api/subscriptions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(subscription.getId().intValue())))
            .andExpect(jsonPath("$.[*].label").value(hasItem(DEFAULT_LABEL.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].api").value(hasItem(DEFAULT_API.toString())));
    }

    @Test
    @Transactional
    public void getSubscription() throws Exception {
        // Initialize the database
        subscriptionRepository.saveAndFlush(subscription);

        // Get the subscription
        restSubscriptionMockMvc.perform(get("/api/subscriptions/{id}", subscription.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(subscription.getId().intValue()))
            .andExpect(jsonPath("$.label").value(DEFAULT_LABEL.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.api").value(DEFAULT_API.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingSubscription() throws Exception {
        // Get the subscription
        restSubscriptionMockMvc.perform(get("/api/subscriptions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateSubscription() throws Exception {
        // Initialize the database
        subscriptionRepository.saveAndFlush(subscription);
        int databaseSizeBeforeUpdate = subscriptionRepository.findAll().size();

        // Update the subscription
        Subscription updatedSubscription = subscriptionRepository.findOne(subscription.getId());
        updatedSubscription
            .label(UPDATED_LABEL)
            .description(UPDATED_DESCRIPTION)
            .api(UPDATED_API);

        restSubscriptionMockMvc.perform(put("/api/subscriptions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedSubscription)))
            .andExpect(status().isOk());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeUpdate);
        Subscription testSubscription = subscriptionList.get(subscriptionList.size() - 1);
        assertThat(testSubscription.getLabel()).isEqualTo(UPDATED_LABEL);
        assertThat(testSubscription.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testSubscription.getApi()).isEqualTo(UPDATED_API);
    }

    @Test
    @Transactional
    public void updateNonExistingSubscription() throws Exception {
        int databaseSizeBeforeUpdate = subscriptionRepository.findAll().size();

        // Create the Subscription

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restSubscriptionMockMvc.perform(put("/api/subscriptions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(subscription)))
            .andExpect(status().isCreated());

        // Validate the Subscription in the database
        List<Subscription> subscriptionList = subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteSubscription() throws Exception {
        // Initialize the database
        subscriptionRepository.saveAndFlush(subscription);
        int databaseSizeBeforeDelete = subscriptionRepository.findAll().size();

        // Get the subscription
        restSubscriptionMockMvc.perform(delete("/api/subscriptions/{id}", subscription.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Subscription> subscriptionList = subscriptionRepository.findAll();
        assertThat(subscriptionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Subscription.class);
        Subscription subscription1 = new Subscription();
        subscription1.setId(1L);
        Subscription subscription2 = new Subscription();
        subscription2.setId(subscription1.getId());
        assertThat(subscription1).isEqualTo(subscription2);
        subscription2.setId(2L);
        assertThat(subscription1).isNotEqualTo(subscription2);
        subscription1.setId(null);
        assertThat(subscription1).isNotEqualTo(subscription2);
    }
}
