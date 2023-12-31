package com.example.homeService.service.impl;

import com.example.homeService.entity.Suggestion;
import com.example.homeService.entity.enu.StatusOrder;
import com.example.homeService.exception.SaveException;
import com.example.homeService.mapper.SuggestionMapper;
import com.example.homeService.repository.SuggestionRepository;
import com.example.homeService.service.SuggestionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@RequiredArgsConstructor
@Service
public class SuggestionServiceImpl implements SuggestionService {
    private final SuggestionRepository repository;

    SuggestionMapper suggestionMapper=new SuggestionMapper();
    @Transactional
    @Override
    public void save(Suggestion suggestion) {
//        Suggestion suggestion = suggestionMapper.convert(suggestionDto);
//        suggestion.setExpert(expertService.findByUsername(suggestionDto.getExpertUsername()).get());
//        suggestion.setOrder(orderService.findById(suggestionDto.getOrderId()).get());
        if (duplicateRequest(suggestion) == false)
            if (checkPrice(suggestion) == false) {
                if (checkDate(suggestion) == false) {
                    if (checkStatusOrder(suggestion) == false) {
                        repository.save(suggestion);
                    } else {
                        throw new SaveException("You cannot submit a request");
                    }
                } else {
                    throw new SaveException("The date cannot be earlier than the order date");
                }
            } else {
                throw new SaveException("The suggested number is greater than the base number");
            }
    }

    @Override
    public boolean duplicateRequest(Suggestion suggestion) {
        List<Suggestion> list = suggestion.getOrder().getSuggestion();
        boolean test = false;
        for (Suggestion s : list) {
            if (s.getExpert() == suggestion.getExpert()) {
                test = true;
            }
        }
        return test;
    }

    @Override
    public boolean checkPrice(Suggestion suggestion) {
        boolean test = false;
        if (suggestion.getPrice() < suggestion.getOrder().getSubService().getBasePrice()) {
            test = true;
        }
        return test;
    }

    @Override
    public boolean checkDate(Suggestion suggestion) {
        boolean test = false;
        if (suggestion.getDate().isBefore(suggestion.getOrder().getDate())) {
            test = true;
        }
        return test;
    }

    @Override
    public boolean checkStatusOrder(Suggestion suggestion) {
        boolean test = true;
        if (suggestion.getOrder().getStatusOrder() == StatusOrder.ExpertSuggestions || suggestion.getOrder().getStatusOrder() == StatusOrder.ExpertSelection) {
            test = false;
        }
        return test;
    }
}

