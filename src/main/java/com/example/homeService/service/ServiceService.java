package com.example.homeService.service;

import com.example.homeService.dto.manager.ServiceDto;
import com.example.homeService.entity.Service;

import java.util.Optional;

public interface ServiceService {
    public boolean repetitive(Service service);
    public void save(ServiceDto serviceDto);
    public void update(Service service);
    public void delete(ServiceDto ServiceDto);
    public Optional<Service> findById(Long Id);
    public Optional<Service> findByTitle(String s);
}
