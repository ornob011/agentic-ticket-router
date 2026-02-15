package com.dsi.support.agenticrouter.service.ai;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import com.dsi.support.agenticrouter.util.StringNormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ModelService {

    private final ModelRegistryRepository modelRegistryRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public String getActiveModelTag() {
        ModelRegistry activeModel = requireSingleActiveModel();
        String activeModelTag = activeModel.getModelTag();

        log.debug(
            "ModelLookup({}) ModelRegistry(tag:{},active:{})",
            OperationalLogContext.PHASE_COMPLETE,
            activeModelTag,
            true
        );

        return activeModelTag;
    }

    @Transactional(readOnly = true)
    public ModelRegistry getActiveModel() {
        ModelRegistry activeModel = requireSingleActiveModel();

        log.info(
            "ModelLookup({}) ModelRegistry(id:{},tag:{},active:{})",
            OperationalLogContext.PHASE_COMPLETE,
            activeModel.getId(),
            activeModel.getModelTag(),
            activeModel.isActive()
        );

        return activeModel;
    }

    @Transactional(readOnly = true)
    public List<ModelRegistry> getAllModels() {
        List<ModelRegistry> models = modelRegistryRepository.findAllByOrderByCreatedAtAsc();

        log.debug(
            "ModelList({}) Outcome(modelCount:{})",
            OperationalLogContext.PHASE_COMPLETE,
            models.size()
        );

        return models;
    }

    public void activateModel(
        String modelTag,
        Long activatorId
    ) {
        log.info(
            "ModelActivate({}) ModelRegistry(tag:{}) Actor(id:{})",
            OperationalLogContext.PHASE_START,
            modelTag,
            activatorId
        );

        String normalizedModelTag = StringNormalizationUtils.trimToNull(modelTag);
        if (normalizedModelTag == null) {
            throw new IllegalArgumentException("modelTag is required");
        }

        AppUser activator = appUserRepository.findById(activatorId)
                                             .orElseThrow(
                                                 DataNotFoundException.supplier(
                                                     AppUser.class,
                                                     activatorId
                                                 )
                                             );

        ModelRegistry modelToActivate = modelRegistryRepository.findByModelTag(normalizedModelTag)
                                                               .orElseThrow(
                                                                   DataNotFoundException.supplier(
                                                                       ModelRegistry.class,
                                                                       normalizedModelTag
                                                                   )
                                                               );

        modelRegistryRepository.findByActiveTrue()
                               .forEach(model -> {
                                   if (model.getId().equals(modelToActivate.getId())) {
                                       return;
                                   }

                                   model.deactivate();
                                   modelRegistryRepository.save(model);
                                   log.debug(
                                       "ModelActivate({}) ModelRegistry(id:{},tag:{},active:{})",
                                       OperationalLogContext.PHASE_PERSIST,
                                       model.getId(),
                                       model.getModelTag(),
                                       model.isActive()
                                   );
                               });

        modelToActivate.activate(activator);
        modelRegistryRepository.save(modelToActivate);

        log.info(
            "ModelActivate({}) ModelRegistry(id:{},tag:{},active:{}) Actor(id:{},role:{})",
            OperationalLogContext.PHASE_COMPLETE,
            modelToActivate.getId(),
            modelToActivate.getModelTag(),
            modelToActivate.isActive(),
            activator.getId(),
            activator.getRole()
        );
    }

    private ModelRegistry requireSingleActiveModel() {
        List<ModelRegistry> activeModels = modelRegistryRepository.findByActiveTrue();

        if (activeModels.isEmpty()) {
            throw new DataNotFoundException(
                ModelRegistry.class,
                "active model"
            );
        }

        ModelRegistry resolvedActiveModel = activeModels.get(0);
        if (activeModels.size() > 1) {
            String activeModelTags = activeModels.stream()
                                                 .map(ModelRegistry::getModelTag)
                                                 .filter(Objects::nonNull)
                                                 .sorted()
                                                 .toList()
                                                 .toString();
            throw new IllegalStateException(
                "Multiple active models configured: " + activeModelTags
            );
        }

        return resolvedActiveModel;
    }
}
