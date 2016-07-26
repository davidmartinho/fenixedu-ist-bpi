package org.fenixedu.academic.bpi.webservice;

import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceConfiguration;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceServerConfiguration;
import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.person.Gender;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.Period;
import org.joda.time.YearMonthDay;

import javax.jws.WebMethod;
import javax.jws.WebService;

import java.util.Locale;

@WebService
public class BPISyncWebService extends BennuWebService {

    @WebMethod
    public BPISyncBean getUser(String fiscalCode) throws BPISyncException{
        Person person = (Person) Person.readByContributorNumber(fiscalCode);

        if(person == null){
            throw new BPISyncException("User not found");
        }

        char gender = person.getGender() == Gender.MALE? 'M': 'F';

        Period age = new Period(person.getDateOfBirthYearMonthDay(), new YearMonthDay());

        if (age.getYears() < 18){
            throw new BPISyncException("User is underage");
        }

        if(person.getUser().getBpiCard() == null || !person.getUser().getBpiCard().getAllowSendDetails()){
            throw new BPISyncException("User does not allow to see details");
        }

        Degree degree = person.getStudent().getLastRegistration().getDegree();

        BPISyncBean bean = new BPISyncBean();
        bean.setFiscal(person.getSocialSecurityNumber());
        bean.setPhone(person.getDefaultPhoneNumber());
        bean.setEmail(person.getDefaultEmailAddressValue());
        bean.setName(person.getName());
        bean.setGender(gender);
        bean.setNationality(person.getCountry().getThreeLetterCode().toCharArray());
        bean.setDateOfBirth(person.getDateOfBirthYearMonthDay().toString("YYMMDD"));
        bean.setIdDocumentNumber(person.getDocumentIdNumber());
        bean.setIdDocumentValidity(person.getExpirationDateOfDocumentIdYearMonthDay().toString("YYMMDD"));
        bean.setPlaceOfBirth(person.getCountryOfBirth().getThreeLetterCode().toCharArray());
        bean.setAddress(person.getAddress());
        bean.setDistrict(person.getDistrictOfResidence());
        bean.setCounty(person.getDistrictSubdivisionOfResidence());
        bean.setBorough(person.getParishOfResidence());

        String postalCode = person.getDefaultPhysicalAddress().getAreaCode();
        if (postalCode.contains("-")){

            String[] codes = postalCode.split("-");
            bean.setZipCode(codes[0]);
            bean.setStreetLayoutCode(codes[1]);
        }else{
            bean.setZipCode(postalCode);
            bean.setStreetLayoutCode("");
        }

        bean.setDegree(degree.getDegreeType().isBolonhaMasterDegree()?"Mestrado":"Licenciatura");
        bean.setDegreeType(degree.getPresentationNameI18N().getContent(new Locale("pt-PT")));
        bean.setId(person.getUsername());
        bean.setEnrolmentAgreement(new byte[0]);

        return bean;
    }

    public static boolean validate(final String username, final String password) {
        final WebServiceConfiguration config =
                WebServiceServerConfiguration.readByImplementationClass(BPISyncWebService.class.getName());
        if (config instanceof WebServiceServerConfiguration) {
            final WebServiceServerConfiguration serverConfig = (WebServiceServerConfiguration) config;
            return username != null && password != null && username.equals(serverConfig.getServiceUsername())
                    && password.equals(serverConfig.getServicePassword());
        }
        return false;
    }
}