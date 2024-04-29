package fi.vm.yti.terminology.api.v2.endpoint;

import fi.vm.yti.common.dto.MetaDataDTO;

public record DataWithError(String errorMessage, MetaDataDTO data) {
}
