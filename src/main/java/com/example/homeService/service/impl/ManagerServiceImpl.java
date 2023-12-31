package com.example.homeService.service.impl;

import com.example.homeService.Validation.Validation;
import com.example.homeService.dto.*;
import com.example.homeService.dto.expert.ExpertDto;
import com.example.homeService.dto.manager.ServiceDto;
import com.example.homeService.dto.manager.SubServiceDto;
import com.example.homeService.entity.*;
import com.example.homeService.entity.enu.StatusExpert;
import com.example.homeService.entity.enu.StatusOrder;
import com.example.homeService.entity.enu.UserRole;
import com.example.homeService.exception.*;
import com.example.homeService.filter.CustomerFilter;
import com.example.homeService.filter.ExpertFilter;
import com.example.homeService.mapper.*;
import com.example.homeService.repository.ManagerRepository;
import com.example.homeService.security.tokan.ConfigurationToken;
import com.example.homeService.security.tokan.ConfigurationTokenService;
import com.example.homeService.service.CustomerService;
import com.example.homeService.service.EmailService;
import com.example.homeService.service.ManagerService;
import com.example.homeService.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ManagerServiceImpl implements ManagerService {
    private final ManagerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SubServiceServiceImpl subServiceServiceImpl;
    private final ServiceServiceImpl serviceServiceImpl;
    private final ExpertServiceImpl expertServiceImpl;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final ConfigurationTokenService configurationTokenService;
    private final EmailService emailService;
    private final RequestExpertServiceImpl requestExpertServiceImpl;
    RequestExpertMapper requestExpertMapper = new RequestExpertMapper();
    CustomerFilter customerFilter = new CustomerFilter();
    ExpertFilter expertFilter = new ExpertFilter();
    ManagerMapper managerMapper = new ManagerMapper();
    CustomerMapper customerMapper=new CustomerMapper();
    ExpertMapper expertMapper=new ExpertMapper();
    OrderMapper orderMapper=new OrderMapper();


    @Override
    public void singup(ManagerDto managerDto) {
        Optional<Manager> manager = findByEmail(managerDto.getEmail());
        if (manager.isPresent()) {
            throw new SaveException("This email is already registered");
        }
        Manager manager1 = managerMapper.convert(managerDto);
        manager1.setPassword(passwordEncoder.encode(managerDto.getPassword()));
        repository.save(manager1);

        String newToken = UUID.randomUUID().toString();
        ConfigurationToken configurationToken = new ConfigurationToken(LocalDateTime.now(), LocalDateTime.now().plusMinutes(15), manager1);
        configurationToken.setToken(newToken);

        configurationTokenService.saveConfigurationToken(configurationToken);
        SimpleMailMessage mailMessage =
                emailService.createEmail(manager1.getEmail(),
                        configurationToken.getToken(), UserRole.MANAGER);
        emailService.sendEmail(mailMessage);

    }

    @Override
    @Transactional
    public void addExpert(String titleSubService, String expertUsername) {
        SubService subService = subServiceServiceImpl.findByTitle(titleSubService).get();
        Expert expert = expertServiceImpl.findByUsername(expertUsername).get();
        subService.getExperts().add(expert);
        subServiceServiceImpl.update(subService);
        System.out.println(expert.getSubServices());
//        expertServiceImpl.update(expert);
    }

    @Override
    @Transactional
    public void removeExpert(String titleSubService, String expertUsername) {

        Expert expert = expertServiceImpl.findByUsername(expertUsername).get();
        List<SubService> subServices = expert.getSubServices();
        SubService subService = subServiceServiceImpl.findByTitle(titleSubService).get();
        System.out.println("-----------------------------");
        System.out.println(expert.getSubServices() + " " + expert.getSubServices().size());
        System.out.println("-----------------------------");
        for (int i = 0; i < subServices.size(); i++) {
            if (subServices.get(i).getTitle().equals(titleSubService)) {
                subServices.remove(i);
                subService.getExperts().remove(i);
            }
        }
        System.out.println(expert.getSubServices() + " " + expert.getSubServices().size());
        expert.setSubServices(subServices);
        expertServiceImpl.update(expert);
        System.out.println("-----------------------------");
        System.out.println(expert.getSubServices() + " " + expert.getSubServices().size());
        System.out.println("-----------------------------");
    }


    @Override
    public Optional<Manager> login(String user, String pass) {

        Optional<Manager> manager;

        manager = repository.findByUsername(user);
        if (manager.isPresent()) {
            if (manager.get().getPassword().equals(pass)) {
                return manager;
            } else {
                throw new WrongException("The username or password is incorrect");
            }
        } else {
            throw new NotFoundException("not found Manager ");
        }
    }

    @Override
    @Transactional
    public void addService(ServiceDto serviceDto) {

        serviceServiceImpl.save(serviceDto);
    }

    @Override
    @Transactional
    public void removeService(ServiceDto serviceDto) {
        serviceServiceImpl.delete(serviceDto);
    }

    @Override
    @Transactional
    public void changePassword(Manager manager, String pass) {
        if (Validation.isValidPassword(pass) == true) {
            manager.setPassword(pass);
            repository.save(manager);
        } else {
            throw new InvalidPasswordException("The password format is not correct !");
        }
    }

    @Override
    @Transactional
    public void addSubService(SubServiceDto subServiceDto) {
        subServiceServiceImpl.save(subServiceDto);
    }

    @Override
    @Transactional
    public void removeSubService(Long id) {
        subServiceServiceImpl.deleteById(id);
    }

    @Override
    @Transactional
    public void changeBasePrice(Long id, double newPrice) {
        SubService byId = subServiceServiceImpl.findById(id).get();
        if (byId != null) {
            byId.setBasePrice(newPrice);
            subServiceServiceImpl.update(byId);
        } else {
            throw new NotFoundException("not found SubService ");
        }
    }

    @Override
    public ListRequestExpertDto showRequestExperts() {
        List<RequestExpert> list = requestExpertServiceImpl.findStatusWaiting();
        List<RequestExpertDto> requestExpertDtos = new ArrayList<>();
        for (RequestExpert r : list) {
            requestExpertDtos.add(requestExpertMapper.convert(r));
        }

        return new ListRequestExpertDto(requestExpertDtos);
    }

    @Override
    @Transactional
    public void changeStatusRequestExpert(Long requestExpertId, Long statusExpert) {
        RequestExpert requestExpert = requestExpertServiceImpl.findById(requestExpertId).get();
        Expert expert = requestExpert.getExpert();
        SubService subService = requestExpert.getSubService();
        if (requestExpert != null) {
            if (statusExpert == 1) {
                requestExpert.setStatusExpert(StatusExpert.accept);
                expert.getSubServices().add(subService);
                subService.getExperts().add(expert);
                subServiceServiceImpl.update(subService);
                requestExpertServiceImpl.save(requestExpert);
                expertServiceImpl.update(expert);
            } else if (statusExpert == 0) {
                requestExpert.setStatusExpert(StatusExpert.Refuse);
                requestExpertServiceImpl.save(requestExpert);
            } else {
                throw new InputeException("The input value is greater than number 1");
            }
        } else {
            throw new NotFoundException("Not found Request");
        }
    }

    @Override
    @Transactional
    public void changeTitleSubService(Long id, String newTitle) {
        SubService byId = subServiceServiceImpl.findById(id).get();
        if (byId != null) {
            byId.setTitle(newTitle);
            subServiceServiceImpl.update(byId);
        } else {
            throw new NotFoundException("not found SubService ");
        }
    }

    @Override
    @Transactional
    public void changeTitleService(Long id, String newTitle) {
        com.example.homeService.entity.Service service = serviceServiceImpl.findById(id).get();
        if (service != null) {
            service.setTitle(newTitle);
            serviceServiceImpl.update(service);
        } else {
            throw new NotFoundException("not found SubService ");
        }
    }

    @Override
    public ListFilterDto filter(FilterDto filterDto) {

        if (filterDto.getFilterEnum().equals(FilterEnum.Customer)) {
            List<Customer> customerList = customerService.finAll();
            return customerFilter.filter(customerList, filterDto);
        } else if (filterDto.getFilterEnum().equals(FilterEnum.Expert)) {


            List<Expert> expertList = expertServiceImpl.findAll();
            return expertFilter.filter(expertList, filterDto);
        } else {
            return null;
        }
    }

    @Override
    public Optional<Manager> findByEmail(String email) {
        Optional<Manager> manager = repository.findByEmail(email);
        return manager;
    }

    @Override
    public ListCustomerDto filterOrderCustomer() {
        List<CustomerDto> customerDtoList=new ArrayList<>();
        List<Customer> customerList = customerFilter.filterByOrder(customerService.finAll());
        for (Customer customer : customerList){
            CustomerDto convert = customerMapper.convert(customer);
            customerDtoList.add(convert);
        }
        return new ListCustomerDto(customerDtoList);
    }

    @Override
    public ListExpertDto filterOrderExpert() {
        List<ExpertDto> list=new ArrayList<>();
        List<Expert> expertList=expertFilter.filterByOrder(expertServiceImpl.findAll());
        for (Expert expert:expertList){
            ExpertDto convert = expertMapper.convert(expert);
            list.add(convert);
        }
        return new ListExpertDto(list);
    }

    @Override
    public ListOrderDto showOrderBetweenDate(LocalDateTime after, LocalDateTime before) {
        List<Order> orders = orderService.OrderBetweenDate(after, before);
        List<OrderDto> orderDtoList=new ArrayList<>();
        for (Order order : orders){
            OrderDto convert = orderMapper.convert(order);
            orderDtoList.add(convert);
        }
        return new ListOrderDto(orderDtoList);
    }

    @Override
    public ListOrderDto showOrdersByStatusOrder(String status) {

        List<OrderDto> orderDtoList=new ArrayList<>();


        StatusOrder statusOrder = null;
        if (status.equals("ExpertSelection")) {
            statusOrder = StatusOrder.ExpertSelection;
        } else if (status.equals("ExpertSuggestions")) {
            statusOrder = StatusOrder.ExpertSuggestions;
        } else if (status.equals("ComingTowardsYou")) {
            statusOrder = StatusOrder.ComingTowardsYou;
        } else if (status.equals("Started")) {
            statusOrder = StatusOrder.Started;
        } else if (status.equals("Done")) {
            statusOrder = StatusOrder.Done;
        } else if (status.equals("Payment")) {
            statusOrder = StatusOrder.Payment;
        } else {
            throw new InputeException("You entered the status name incorrectly!");
        }

        List<Order> orders = orderService.findByStatusOrder(statusOrder);
        for (Order order : orders){
            OrderDto convert = orderMapper.convert(order);
            orderDtoList.add(convert);
        }
        return new ListOrderDto(orderDtoList);
    }

    @Override
    public ListOrderDto showOrderBySubService(String subServiceTitle) {
        List<OrderDto> orderDtoList=new ArrayList<>();

        Optional<SubService> subService = subServiceServiceImpl.findByTitle(subServiceTitle);
        if(subService.isEmpty()){
            throw new InputeException("You entered a blank entry !");
        }
        List<Order> orders = orderService.findBySubService(subService.get());
        for (Order order : orders){
            OrderDto convert = orderMapper.convert(order);
            orderDtoList.add(convert);
        }
        return new ListOrderDto(orderDtoList);
    }

    @Override
    public ListCustomerDto showCustomerByConfirmedAt(LocalDateTime after, LocalDateTime before) {
        List<CustomerDto> customerDtoList=new ArrayList<>();

        List<ConfigurationToken> list =
                configurationTokenService.findByConfirmedAt(after, before);
        for (ConfigurationToken token : list){
            CustomerDto convert = customerMapper.convert(token.getPerson());
            customerDtoList.add(convert);
        }
        return new ListCustomerDto(customerDtoList);
    }
}
