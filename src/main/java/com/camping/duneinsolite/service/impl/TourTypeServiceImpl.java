package com.camping.duneinsolite.service.impl;


import com.camping.duneinsolite.dto.request.TourTypeRequest;
import com.camping.duneinsolite.dto.response.TourTypeResponse;
import com.camping.duneinsolite.mapper.TourTypeMapper;
import com.camping.duneinsolite.model.TourType;
import com.camping.duneinsolite.repository.TourTypeRepository;
import com.camping.duneinsolite.service.TourTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TourTypeServiceImpl implements TourTypeService {

    private final TourTypeRepository tourTypeRepository;
    private final TourTypeMapper tourTypeMapper;

    @Override
    public TourTypeResponse createTourType(TourTypeRequest request) {
        if (tourTypeRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tour type already exists: " + request.getName());
        }
        return tourTypeMapper.toResponse(tourTypeRepository.save(tourTypeMapper.toEntity(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public TourTypeResponse getTourTypeById(UUID tourTypeId) {
        return tourTypeMapper.toResponse(findById(tourTypeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourTypeResponse> getAllTourTypes() {
        return tourTypeRepository.findAll().stream()
                .map(tourTypeMapper::toResponse).toList();
    }

    @Override
    public TourTypeResponse updateTourType(UUID tourTypeId, TourTypeRequest request) {
        TourType tourType = findById(tourTypeId);
        tourType.setName(request.getName());
        tourType.setDescription(request.getDescription());
        tourType.setDuration(request.getDuration());
        tourType.setPassengerAdultPrice(request.getPassengerAdultPrice());
        tourType.setPassengerChildPrice(request.getPassengerChildPrice());
        tourType.setPartnerAdultPrice(request.getPartnerAdultPrice());
        tourType.setPartnerChildPrice(request.getPartnerChildPrice());
        return tourTypeMapper.toResponse(tourTypeRepository.save(tourType));
    }

    @Override
    public void deleteTourType(UUID tourTypeId) {
        tourTypeRepository.delete(findById(tourTypeId));
    }

    private TourType findById(UUID tourTypeId) {
        return tourTypeRepository.findById(tourTypeId)
                .orElseThrow(() -> new RuntimeException("TourType not found: " + tourTypeId));
    }
}
