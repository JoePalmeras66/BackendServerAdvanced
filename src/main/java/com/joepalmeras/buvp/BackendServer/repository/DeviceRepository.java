package com.joepalmeras.buvp.BackendServer.repository;

import com.joepalmeras.buvp.BackendServer.domain.Device;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface DeviceRepository extends PagingAndSortingRepository<Device, Long>, 
        JpaSpecificationExecutor<Device> {
}
