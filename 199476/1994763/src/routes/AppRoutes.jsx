import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage.jsx';
import RegisterPage from '../pages/auth/RegisterPage.jsx';
import LegalPage from '../pages/auth/LegalPage.jsx';
import HomePage from '../pages/home/HomePage.jsx';
import KnowledgePage from '../pages/discovery/KnowledgePage.jsx';
import ExperiencePage from '../pages/discovery/ExperiencePage.jsx';
import FilterPage from '../pages/discovery/FilterPage.jsx';
import TalentPage from '../pages/talent/TalentPage.jsx';
import MyInquiriesPage from '../pages/inquiries/MyInquiriesPage.jsx';
import DirectChatPage from '../pages/messages/DirectChatPage.jsx';
import MyProfilePage from '../pages/profile/ProfilePage.jsx';
import AccountSettingsPage from '../pages/profile/AccountSettingsPage.jsx';
import WalletPage from '../pages/profile/WalletPage.jsx';
import CertificationPage from '../pages/certification/CertificationPage.jsx';
import WorkCertificationPage from '../pages/certification/WorkCertificationPage.jsx';
import ExperienceCertificationPage from '../pages/certification/ExperienceCertificationPage.jsx';
import BasicCertificationApplyPage from '../pages/certification/BasicCertificationApplyPage.jsx';
import ExperienceCertificationApplyPage from '../pages/certification/ExperienceCertificationApplyPage.jsx';
import NoticesPage from '../pages/notifications/NoticesPage.jsx';
import FeedbackPage from '../pages/support/FeedbackPage.jsx';
import FaqPage from '../pages/support/FaqPage.jsx';
import BusinessCooperationPage from '../pages/support/BusinessCooperationPage.jsx';
import CustomerServicePage from '../pages/support/CustomerServicePage.jsx';
import { ROUTES } from './routeConfig.js';

export default function AppRoutes(props) {
  const { go, notify } = props;
  const protect = (element) =>
    props.isAuthenticated ? element : <Navigate to={ROUTES.login} replace />;
  return (
    <Routes>
      <Route
        path={ROUTES.login}
        element={
          props.isAuthenticated ? (
            <Navigate to={ROUTES.home} replace />
          ) : (
            <LoginPage go={go} onLogin={props.login} notify={notify} />
          )
        }
      />
      <Route
        path={ROUTES.register}
        element={
          props.isAuthenticated ? (
            <Navigate to={ROUTES.home} replace />
          ) : (
            <RegisterPage go={go} onRegister={props.login} notify={notify} />
          )
        }
      />
      <Route path={ROUTES.terms} element={<LegalPage go={go} type="terms" />} />
      <Route path={ROUTES.privacy} element={<LegalPage go={go} type="privacy" />} />
      <Route
        path={ROUTES.home}
        element={protect(
          <HomePage
            go={go}
            setTalent={props.setTalent}
            unreadNoticeCount={props.unreadNoticeCount}
            answerers={props.answerers}
          />,
        )}
      />
      <Route
        path={ROUTES.talent}
        element={protect(
          <TalentPage
            go={go}
            talent={props.talent}
            conversations={props.conversations}
            setConversations={props.setConversations}
            setSelectedConversation={props.setSelectedConversation}
            balance={props.balance}
            setBalance={props.setBalance}
            problem={props.problem}
            experience={props.experience}
            addNotice={props.addNotice}
          />,
        )}
      />
      <Route
        path={ROUTES.knowledge}
        element={
          <KnowledgePage
            go={go}
            category={props.category}
            setCategory={props.setCategory}
            problem={props.problem}
            setProblem={props.setProblem}
            matterId={props.matterId}
            setMatterId={props.setMatterId}
            setExperience={props.setExperience}
            setExperienceCategoryId={props.setExperienceCategoryId}
            catalog={props.discoveryCatalog}
            refreshCatalog={props.refreshDiscoveryCatalog}
          />
        }
      />
      <Route
        path={ROUTES.experiences}
        element={
          <ExperiencePage
            go={go}
            experience={props.experience}
            experienceCategoryId={props.experienceCategoryId}
            setExperience={props.setExperience}
            setProblem={props.setProblem}
            category={props.category}
            setCategory={props.setCategory}
            setExperienceCategoryId={props.setExperienceCategoryId}
            catalog={props.discoveryCatalog}
            refreshCatalog={props.refreshDiscoveryCatalog}
          />
        }
      />
      <Route
        path={ROUTES.filtered}
        element={
          <FilterPage
            go={go}
            problem={props.problem}
            matterId={props.matterId}
            experience={props.experience}
            experienceCategoryId={props.experienceCategoryId}
            setTalent={props.setTalent}
            title={props.experience ? '按经历找人' : '按事情找人'}
            backScreen={props.experience ? 'experiences' : 'knowledge'}
            answerers={props.answerers}
            catalog={props.discoveryCatalog}
          />
        }
      />
      <Route
        path={ROUTES.inquiries}
        element={
          <MyInquiriesPage
            go={go}
            conversations={props.conversations}
            setConversations={props.setConversations}
            setSelectedConversation={props.setSelectedConversation}
          />
        }
      />
      <Route
        path={ROUTES.directChat}
        element={protect(
          <DirectChatPage
            go={go}
            conversation={props.selectedConversation}
            setConversations={props.setConversations}
            setSelectedConversation={props.setSelectedConversation}
            currentUser={props.userProfile}
            canAnswer={props.canAnswer}
            acceptingInquiries={props.acceptingInquiries}
            certifications={props.certifications}
            addNotice={props.addNotice}
          />,
        )}
      />
      <Route
        path={ROUTES.profile}
        element={protect(
          <MyProfilePage
            go={go}
            certifications={props.certifications}
            logout={props.logout}
            userProfile={props.userProfile}
          />,
        )}
      />
      <Route
        path={ROUTES.accountSettings}
        element={protect(
          <AccountSettingsPage
            go={go}
            notify={notify}
            userProfile={props.userProfile}
            setUserProfile={props.setUserProfile}
            conversations={props.conversations}
            balance={props.balance}
            frozenAmount={props.frozenAmount}
            deleteAccount={props.deleteAccount}
          />,
        )}
      />
      <Route
        path={ROUTES.wallet}
        element={protect(
          <WalletPage
            go={go}
            notify={notify}
            balance={props.balance}
            setBalance={props.setBalance}
            ledger={props.ledger}
            setLedger={props.setLedger}
            withdrawals={props.withdrawals}
            setWithdrawals={props.setWithdrawals}
            accountStats={props.accountStats}
            setAccountStats={props.setAccountStats}
            frozenAmount={props.frozenAmount}
          />,
        )}
      />
      <Route
        path={ROUTES.certs}
        element={
          <CertificationPage
            go={go}
            certifications={props.certifications}
            acceptingInquiries={props.acceptingInquiries}
            setAcceptingInquiries={props.setAcceptingInquiries}
          />
        }
      />
      <Route
        path={ROUTES.certWork}
        element={
          <WorkCertificationPage
            go={go}
            setCertType={props.setCertType}
            certifications={props.certifications}
          />
        }
      />
      <Route
        path={ROUTES.certExperience}
        element={
          <ExperienceCertificationPage
            go={go}
            setCertType={props.setCertType}
            certifications={props.certifications}
            setCertifications={props.setCertifications}
          />
        }
      />
      <Route
        path={ROUTES.certBasicApply}
        element={
          <BasicCertificationApplyPage
            go={go}
            certId={props.certType}
            certifications={props.certifications}
            setCertifications={props.setCertifications}
            notify={notify}
            addNotice={props.addNotice}
          />
        }
      />
      <Route
        path={ROUTES.certExperienceApply}
        element={
          <ExperienceCertificationApplyPage
            go={go}
            certId={props.certType}
            certifications={props.certifications}
            setCertifications={props.setCertifications}
            notify={notify}
            addNotice={props.addNotice}
          />
        }
      />
      <Route
        path={ROUTES.feedback}
        element={
          <FeedbackPage
            go={go}
            notify={notify}
            conversations={props.conversations}
            records={props.feedbackRecords}
            setRecords={props.setFeedbackRecords}
          />
        }
      />
      <Route path={ROUTES.faq} element={protect(<FaqPage go={go} />)} />
      <Route
        path={ROUTES.business}
        element={protect(<BusinessCooperationPage go={go} notify={notify} />)}
      />
      <Route path={ROUTES.customerService} element={protect(<CustomerServicePage go={go} />)} />
      <Route
        path={ROUTES.notices}
        element={<NoticesPage go={go} notices={props.notices} setNotices={props.setNotices} />}
      />
      <Route
        path="*"
        element={<Navigate to={props.isAuthenticated ? ROUTES.home : ROUTES.login} replace />}
      />
    </Routes>
  );
}
