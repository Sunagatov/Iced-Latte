package com.zufar.icedlatte.user.api.avatar;

import com.zufar.icedlatte.common.converter.FileMetadataDtoConverter;
import com.zufar.icedlatte.common.dto.FileMetadataDto;
import com.zufar.icedlatte.common.entity.FileMetadata;
import com.zufar.icedlatte.user.api.SingleUserProvider;
import com.zufar.icedlatte.user.entity.UserEntity;
import com.zufar.icedlatte.user.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataDtoConverter fileMetadataDtoConverter;
    private final SingleUserProvider singleUserProvider;

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FileMetadataDto save (final FileMetadataDto fileMetadataDto, final UUID userId) {
        FileMetadata fileMetadata = fileMetadataDtoConverter.toEntity(fileMetadataDto);
        FileMetadata savedFileMetadata = fileMetadataRepository.save(fileMetadata);
        UserEntity userEntity = singleUserProvider.getUserEntityById(userId);
        userEntity.setFileMetadata(savedFileMetadata);
        return fileMetadataDtoConverter.toDto(savedFileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public FileMetadata update (final FileMetadata fileMetadata) {
        return fileMetadataRepository.save(fileMetadata);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void delete(final UUID fileId) {
        fileMetadataRepository.deleteById(fileId);
    }
}
