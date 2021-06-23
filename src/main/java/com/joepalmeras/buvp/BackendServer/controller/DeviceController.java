package com.joepalmeras.buvp.BackendServer.controller;

import com.joepalmeras.buvp.BackendServer.domain.Device;
import com.joepalmeras.buvp.BackendServer.exception.BadResourceException;
import com.joepalmeras.buvp.BackendServer.exception.ResourceNotFoundException;
import com.joepalmeras.buvp.BackendServer.service.DeviceService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DeviceController<S extends Session> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String ACTIVE_DEVICES = "activeDevices";
    private static final String ERROR_MESSAGE = "errorMessage";
    private final int ROW_PER_PAGE = 5;
    
    @Autowired
    private DeviceService deviceService;
    
    @Autowired
    private JdbcOperationsSessionRepository sessionService;
    
    @GetMapping(value = {"/", "/index"})
    public String getDevices(Model model, 
    	@RequestParam(value = "page", defaultValue = "1") int pageNumber, 
    	HttpSession session) {
        
    	Session foundSession = sessionService.findById(session.getId());
    	
    	List<Device> usedDevices = foundSession.getAttribute(ACTIVE_DEVICES);
    	
    	List<Device> unusedDevices = deviceService.findAll(pageNumber, ROW_PER_PAGE)
    			.stream()
    			.filter(d -> !d.isUsed())
    			.collect(Collectors.toList());
    	
        long count = deviceService.count();
        boolean hasPrev = pageNumber > 1;
        boolean hasNext = (pageNumber * ROW_PER_PAGE) < count;
        model.addAttribute("devices", unusedDevices);
        model.addAttribute("usedDevices", usedDevices);
        model.addAttribute("hasPrev", hasPrev);
        model.addAttribute("prev", pageNumber - 1);
        model.addAttribute("hasNext", hasNext);
        model.addAttribute("next", pageNumber + 1);
        return "index";
    }

    @GetMapping(value = "/index/{deviceId}")
    public String getDeviceById(Model model, 
    	@PathVariable long deviceId, 
    	HttpSession session) throws ResourceNotFoundException {
        
    	Session foundSession = sessionService.findById(session.getId());
    	List<Device> sessionDevices = foundSession.getAttribute(ACTIVE_DEVICES);
    	
    	Optional<Device> optDevice = sessionDevices.stream()
    			.filter(d -> d.getId() == deviceId)
    			.findFirst();
    	
    	optDevice.orElseThrow(() -> new ResourceNotFoundException("GET - /devices/"+ deviceId +" - Device not found"));
        Device device = optDevice.get();
    	
        model.addAttribute("device", device);
        
        logger.info("CONNECTED Device: " + device.getTyp() + " with sessionID: " + session.getId());
            
        return "device";
    }
    
    /**********************************************************************************/
    /*                                                                                */
    /* POST Request to detach spezific Device                                        */
    /*                                                                                */
    /**********************************************************************************/
    @PostMapping(value = {"/index/{deviceId}"})
    public String detachDeviceById(Model model,
          @PathVariable long deviceId,
          HttpServletRequest request) throws ResourceNotFoundException, BadResourceException {
    	
    	HttpSession session = request.getSession();
    	
    	Session foundSession = sessionService.findById(session.getId());
    	List<Device> sessionDevices = foundSession.getAttribute(ACTIVE_DEVICES);
    	
    	Optional<Device> optDevice = sessionDevices.stream()
    			.filter(d -> d.getId() == deviceId)
    			.findFirst();
    	
    	optDevice.orElseThrow(() -> new ResourceNotFoundException("POST - /devices/"+ deviceId +" - Device not found"));
        Device device = optDevice.get();
    	
        Optional<Principal> optPrincipal = Optional.of(request.getUserPrincipal());
        optPrincipal.orElseThrow(() -> new ResourceNotFoundException("Principal not found!"));

        Principal principal = optPrincipal.get();
        
    	if(principal.getName().compareToIgnoreCase(device.getPrincipal())==0) {
    		//
    		// Only if current 'User' is equal to 'User' who connected the device 
    		// is allowed to detach the device
    		//
    		model.addAttribute("allowDelete", true);
    		
    		device.setUsed(false);
            deviceService.update(device);
            
            sessionDevices.remove(device);
            session.setAttribute(ACTIVE_DEVICES, sessionDevices);
    	}
    	else
    	{
    		model.addAttribute("allowDelete", false);
    		
    		String errorMessage = "Device is in USE from Principal: " + device.getPrincipal();
    		model.addAttribute(ERROR_MESSAGE, errorMessage);
    	}
    	
    	return "index";
    }

    @GetMapping(value = {"/index/add"})
    public String showAddDevice(Model model,    		
    		HttpSession session) {     
	     
    	List<Device> devices = getSessionDevices(session);
    	
	    model.addAttribute("add", true);    
	    model.addAttribute("device", new Device());
	    session.setAttribute(ACTIVE_DEVICES, devices);

	    return "device-edit";
    }
  
    /**********************************************************************************/
    /*                                                                                */
    /* POST Request to connect spezific Device                                        */
    /*                                                                                */
    /**********************************************************************************/
    @PostMapping(value = "/index/add")
    public String addDevice(Model model,
            @ModelAttribute("device") Device device, 
            HttpServletRequest request) { 
    	
        try {
        	
        	HttpSession session = request.getSession();
        	List<Device> sessionDevices = getSessionDevices(session);
        	
        	Optional<Device> optMatchedDevice = matchedDevises(device);        			
        	        	
        	optMatchedDevice.orElseThrow(() -> new ResourceNotFoundException("DEVICE not matched !"));
        	
        	boolean bUsed = deviceIsInUse(sessionDevices, optMatchedDevice.get());
        	
        	if(!bUsed) {
        		
        		Optional<Principal> principal = Optional.of(request.getUserPrincipal());
    	    	principal.orElseThrow(() -> new ResourceNotFoundException("Principal not found!"));
            	        		
        		device = optMatchedDevice.get();
        		device.setPrincipal(principal.get().getName());
        		device.setUsed(true);
        		
        		deviceService.update(device);
        		
        		sessionDevices.add(device);        
                session.setAttribute(ACTIVE_DEVICES, sessionDevices);
                
	            Device newDevice = deviceService.findById(device.getId());
	            
	            String errorMessage = "START SESSION with " + "Device: " + newDevice.getTyp() + " at Location: " + newDevice.getLocation() + " with sessionId: " + session.getId();
//	            model.addAttribute(ERROR_MESSAGE, errorMessage);
	            logger.info(errorMessage);
	            
	            return "redirect:/index/" + String.valueOf(newDevice.getId());
        	}
        	else {
        		String sessionInUseMessage = "USED Device: " + device.getTyp() + " at Location: " + device.getLocation() + " in use.";
        		model.addAttribute(ERROR_MESSAGE, sessionInUseMessage);
        		
        		logger.info(sessionInUseMessage);
        		
                model.addAttribute("add", true);
                return "device-edit";
        	}
        	
        } catch (Exception ex) {
            
        	String errorMessage = ex.getMessage();
        	
            model.addAttribute(ERROR_MESSAGE, errorMessage);
            model.addAttribute("add", true);
            logger.info(errorMessage);
            
            return "device-edit";
        }        
    }

    @GetMapping(value = {"/index/{deviceId}/delete"})
    public String showDeleteDeviceById(
            Model model, 
            @PathVariable long deviceId, 
            HttpSession session) throws ResourceNotFoundException, BadResourceException {    	    	
    	
    	Optional<Session> optFoundSession = Optional.of(sessionService.findById(session.getId()));
    	
    	optFoundSession.orElseThrow(() -> new ResourceNotFoundException("No Session Devices found !"));
    	
    	Session foundSession = optFoundSession.get();
    	List<Device> sessionDevices = foundSession.getAttribute(ACTIVE_DEVICES);
    	
    	Optional<Device> optDevice = sessionDevices.stream()
    			.filter(d -> d.getId() == deviceId)
    			.findFirst();
    	
    	Device device = optDevice.orElse(null);
    	
        if((device != null) && device.isUsed()) {
        	device.setPrincipal(null);
        	device.setUsed(false);
        	deviceService.update(device);
        }
        
        model.addAttribute("allowDelete", true);
        model.addAttribute("device", device);
        
        return "device";
    }

    /**********************************************************************************/
    /*                                                                                */
    /* POST Request to detach spezific Device                                         */
    /*                                                                                */
    /**********************************************************************************/
    @PostMapping(value = {"/index/{deviceId}/delete"})
    public String deleteDeviceById(
            Model model, @PathVariable long deviceId, 
            HttpServletRequest request) {
    	
        try {
        	
        	HttpSession session = request.getSession();
        
        	Optional<Session> optFoundSession = Optional.of(sessionService.findById(session.getId()));
        	
        	optFoundSession.orElseThrow(() -> new ResourceNotFoundException("No Session Devices found !"));
        	
        	Session foundSession = optFoundSession.get();
        	List<Device> sessionDevices = foundSession.getAttribute(ACTIVE_DEVICES);
        	
        	Optional<Device> optDevice = sessionDevices.stream()
        			.filter(d -> d.getId() == deviceId)
        			.findFirst();
        	
        	Device device = optDevice.get();
        	
    		Optional<Principal> optPrincipal = Optional.of(request.getUserPrincipal());
	        optPrincipal.orElseThrow(() -> new ResourceNotFoundException("Principal not found!"));

	        Principal principal = optPrincipal.get();

	        logger.info("Principal User Name: " + principal.getName());
	        logger.info("Device User Name: " + device.getPrincipal());
	        
	        if(principal.getName().compareToIgnoreCase(device.getPrincipal())==0) {
	        	//
	    		// Only if current 'User' is equal to 'User' who connected the device 
	    		// is allowed to detach the device
	    		//
	        	model.addAttribute("allowDelete", true);
	        	
	        	device.setPrincipal(null);
	        	device.setUsed(false);

	            deviceService.update(device);
	            
	            sessionDevices.remove(device);
	            session.setAttribute(ACTIVE_DEVICES, sessionDevices);
	        	
	        	String errorMessage = "DETACH SESSION with" + " Device: " + device.getTyp() + " at Location: " + device.getLocation() + " with sessionId: " + session.getId();
//				model.addAttribute(ERROR_MESSAGE, errorMessage);
				logger.info(errorMessage);
				
				return "redirect:/index";
	        }   
	        else
	    	{
	    		String errorMessage = "Device is in USE from Principal: " + device.getPrincipal();
	    		model.addAttribute(ERROR_MESSAGE, errorMessage);
	    		
	    		model.addAttribute("allowDelete", false);
	    		
	    		return "device";
	    	}
			
            
        } catch (ResourceNotFoundException | BadResourceException ex) {
            String errorMessage = ex.getMessage();
            logger.error(errorMessage);
            model.addAttribute(ERROR_MESSAGE, errorMessage);
            return "device";
        }
    }
    
    private List<Device> getSessionDevices(HttpSession session) {
        List<Device> devices = (List<Device>) session.getAttribute(ACTIVE_DEVICES);
        if (devices == null) {
        	devices = new ArrayList<>();
        }
        return devices;
    }
    
    /**********************************************************************************/
    /*                                                                                */
    /* Helper function to look if requested Device is not in use in another Session   */
    /*                                                                                */
    /**********************************************************************************/
    private boolean deviceIsInUse(List<Device> sessionDevices, Device device) {
    	
    	Predicate<Device> typesAreEqual = e -> (e.getTyp().toUpperCase().compareTo(device.getTyp().toUpperCase())==0);
    	Predicate<Device> locationAreEqual = e -> (e.getLocation().toUpperCase().compareTo(device.getLocation().toUpperCase())==0);
    	Predicate<Device> equipmentAreEqual = e -> (e.getEquipment().toUpperCase().compareTo(device.getEquipment().toUpperCase())==0);
    	Predicate<Device> energyAreEqual = e -> (e.getEnergy() >= device.getEnergy());

    	Predicate<Device> combinedCondition = typesAreEqual
    			.and(locationAreEqual)
    			.and(equipmentAreEqual)
    			.and(energyAreEqual);

    	boolean isInUse = sessionDevices.stream()
    						.anyMatch(combinedCondition);
    	
    	
    	return isInUse;
    }
    
    /****************************************************************/
    /*                                                              */
    /* Helper function to look if requested Device is available     */
    /*                                                              */
    /****************************************************************/
    private Optional<Device> matchedDevises(Device device) {
    	
    	List<Device> allDevices = deviceService.findAll(1, ROW_PER_PAGE);
    	
    	Predicate<Device> typesAreEqual = e -> (e.getTyp().toUpperCase().compareTo(device.getTyp().toUpperCase())==0);
    	Predicate<Device> locationAreEqual = e -> (e.getLocation().toUpperCase().compareTo(device.getLocation().toUpperCase())==0);
    	Predicate<Device> equipmentAreEqual = e -> (e.getEquipment().toUpperCase().compareTo(device.getEquipment().toUpperCase())==0);
    	Predicate<Device> energyAreEqual = e -> (e.getEnergy() >= device.getEnergy());
    	Predicate<Device> freeDevice = e -> (!e.isUsed());
    	
    	Predicate<Device> combinedCondition = typesAreEqual
    			.and(locationAreEqual)
    			.and(equipmentAreEqual)
    			.and(energyAreEqual)
    			.and(freeDevice);
    	
    	return allDevices.stream()
    			.filter(combinedCondition)
    			.findFirst();    	
    }
    
}
