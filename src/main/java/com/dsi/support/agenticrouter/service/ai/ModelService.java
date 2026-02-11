package com.dsi.support.agenticrouter.service.ai;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
import com.dsi.support.agenticrouter.util.OperationalLogContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ModelService {

    private final ModelRegistryRepository modelRegistryRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public String getActiveModelTag() {
        String activeModelTag = modelRegistryRepository.findByActiveTrue()
                                                       .stream()
                                                       .findFirst()
                                                       .map(ModelRegistry::getModelTag)
                                                       .orElseThrow(
                                                           DataNotFoundException.supplier(
                                                               ModelRegistry.class,
                                                               "active model"
                                                           )
                                                       );

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
        ModelRegistry activeModel = modelRegistryRepository.findByActiveTrue()
                                                           .stream()
                                                           .findFirst()
                                                           .orElseThrow(
                                                               DataNotFoundException.supplier(
                                                                   ModelRegistry.class,
                                                                   "active model"
                                                               )
                                                           );

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
        List<ModelRegistry> models = modelRegistryRepository.findAll();

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

        AppUser activator = appUserRepository.findById(activatorId)
                                             .orElseThrow(
                                                 DataNotFoundException.supplier(
                                                     AppUser.class,
                                                     activatorId
                                                 )
                                             );

        modelRegistryRepository.findByActiveTrue()
                               .forEach(model -> {
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

        ModelRegistry modelToActivate = modelRegistryRepository.findByModelTag(modelTag)
                                                               .orElseThrow(
                                                                   DataNotFoundException.supplier(
                                                                       ModelRegistry.class,
                                                                       modelTag
                                                                   )
                                                               );

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
}
