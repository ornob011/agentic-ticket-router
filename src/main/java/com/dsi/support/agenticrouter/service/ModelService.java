package com.dsi.support.agenticrouter.service;

import com.dsi.support.agenticrouter.entity.AppUser;
import com.dsi.support.agenticrouter.entity.ModelRegistry;
import com.dsi.support.agenticrouter.exception.DataNotFoundException;
import com.dsi.support.agenticrouter.repository.AppUserRepository;
import com.dsi.support.agenticrouter.repository.ModelRegistryRepository;
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
        return modelRegistryRepository.findByActiveTrue()
                                      .stream()
                                      .findFirst()
                                      .map(ModelRegistry::getModelTag)
                                      .orElseThrow(
                                          DataNotFoundException.supplier(
                                              ModelRegistry.class,
                                              "active model"
                                          )
                                      );
    }

    @Transactional(readOnly = true)
    public ModelRegistry getActiveModel() {
        return modelRegistryRepository.findByActiveTrue()
                                      .stream()
                                      .findFirst()
                                      .orElseThrow(
                                          DataNotFoundException.supplier(
                                              ModelRegistry.class,
                                              "active model"
                                          )
                                      );
    }

    @Transactional(readOnly = true)
    public List<ModelRegistry> getAllModels() {
        return modelRegistryRepository.findAll();
    }

    public void activateModel(
        String modelTag,
        Long activatorId
    ) {
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
    }
}
