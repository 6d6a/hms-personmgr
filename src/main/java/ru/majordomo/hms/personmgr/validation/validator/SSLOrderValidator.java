package ru.majordomo.hms.personmgr.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateOrder;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateProduct;
import ru.majordomo.hms.personmgr.model.order.ssl.SslCertificateServerType;
import ru.majordomo.hms.personmgr.validation.ValidSSLOrder;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Component
public class SSLOrderValidator implements ConstraintValidator<ValidSSLOrder, SslCertificateOrder> {
    private final MongoOperations operations;

    @Autowired
    public SSLOrderValidator(MongoOperations operations) {
        this.operations = operations;
    }

    @Override
    public void initialize(ValidSSLOrder constraintAnnotation) {}

    @Override
    public boolean isValid(SslCertificateOrder order, ConstraintValidatorContext context) {
        SslCertificateProduct product = operations.findById(order.getProductId(), SslCertificateProduct.class);

        if (product == null) {
            return false;
        }

        SslCertificateServerType serverType = operations.findById(
                order.getServerTypeId(), SslCertificateServerType.class
        );

        if (serverType == null) {
            return false;
        }

        if (!serverType.getSupplierId().equals(product.getSupplierId())) {
            return false;
        }

        if (order.getIsOrganization()) {
            if (order.getOrganizationName() == null
                    || order.getOrganizationName().isEmpty()
                    || order.getDivision() == null
                    || order.getDivision().isEmpty()
            ) {
                return false;
            }
        } else if (product.isOnlyOrganizations()) {
            return false;
        }

        return true;
    }
}
