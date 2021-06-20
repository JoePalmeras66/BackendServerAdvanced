package com.joepalmeras.buvp.BackendServer.service;

import com.joepalmeras.buvp.BackendServer.domain.Device;
import com.joepalmeras.buvp.BackendServer.exception.BadResourceException;
import com.joepalmeras.buvp.BackendServer.exception.ResourceAlreadyExistsException;
import com.joepalmeras.buvp.BackendServer.exception.ResourceNotFoundException;
import com.joepalmeras.buvp.BackendServer.repository.DeviceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DeviceService {
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    private boolean existsById(Long id) {
        return deviceRepository.existsById(id);
    }
    
    public Device findById(Long id) throws ResourceNotFoundException {
        Device device = deviceRepository.findById(id).orElse(null);
        if (device==null) {
            throw new ResourceNotFoundException("Cannot find Device with id: " + id);
        }
        else return device;
    }
    
    public List<Device> findAll(int pageNumber, int rowPerPage) {
        List<Device> device = new ArrayList<>();
        Pageable sortedByIdAsc = PageRequest.of(pageNumber - 1, rowPerPage, 
                Sort.by("id").ascending());
        deviceRepository.findAll(sortedByIdAsc).forEach(device::add);
        return device;
    }
    
    public Device save(Device device) throws BadResourceException, ResourceAlreadyExistsException {
        if (!device.getTyp().isEmpty()) {
            if (device.getId() != null && existsById(device.getId())) { 
                throw new ResourceAlreadyExistsException("Device with id: " + device.getId() +
                        " already exists");
            }
            return deviceRepository.save(device);
        }
        else {
            BadResourceException exc = new BadResourceException("Failed to save device");
            exc.addErrorMessage("Device is null or empty");
            throw exc;
        }
    }
    
    public void update(Device device) 
            throws BadResourceException, ResourceNotFoundException {
        if (!device.getTyp().isEmpty()) {
            if (!existsById(device.getId())) {
                throw new ResourceNotFoundException("Cannot find Device with id: " + device.getId());
            }
            deviceRepository.save(device);
        }
        else {
            BadResourceException exc = new BadResourceException("Failed to save device");
            exc.addErrorMessage("Device is null or empty");
            throw exc;
        }
    }
    
    public void deleteById(Long id) throws ResourceNotFoundException {
        if (!existsById(id)) { 
            throw new ResourceNotFoundException("Cannot find device with id: " + id);
        }
        else {
            deviceRepository.deleteById(id);
        }
    }
    
    public Long count() {
        return deviceRepository.count();
    }
}
