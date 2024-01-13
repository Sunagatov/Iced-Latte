package com.zufar.icedlatte.order.api;

import com.zufar.icedlatte.openapi.dto.AddedOrder;
import com.zufar.icedlatte.openapi.dto.UpdateOrderStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUpdater {
    public AddedOrder updateStatus(UpdateOrderStatusDto orderInfo) {
        throw new NotImplementedException("updateStatus is not supported yet");
    }
}
